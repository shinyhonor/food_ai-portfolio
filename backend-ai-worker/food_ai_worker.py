import json
import os
import tempfile
import uuid
import boto3
import cv2
import time
import logging
from confluent_kafka import Consumer, Producer, KafkaError
from ultralytics import YOLO
from botocore.client import Config

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
logger = logging.getLogger(__name__)


class FoodAIWorker:
    def __init__(self):
        # 인프라 설정
        # localhost 대신 127.0.0.1 사용 (IPv6 충돌 방지)
        self.bootstrap_servers = '127.0.0.1:9092'
        self.input_topic = 'analysis-request-topic'
        self.result_topic = 'analysis-result-topic'
        self.bucket_name = 'food-ai-bucket'

        # S3 클라이언트
        self.s3_client = boto3.client('s3',
                                      endpoint_url='http://127.0.0.1:9000',
                                      aws_access_key_id='minioadmin',
                                      aws_secret_access_key='minioadmin',
                                      config=Config(signature_version='s3v4', connect_timeout=5,
                                                    retries={'max_attempts': 3})
                                      )

        self.model = YOLO('./model/best_yolov8n.pt')

        # Confluent Consumer 설정
        self.consumer = Consumer({
            'bootstrap.servers': self.bootstrap_servers,
            'group.id': 'food-ai-inference-group',
            'auto.offset.reset': 'earliest',
            'enable.auto.commit': True
        })
        self.consumer.subscribe([self.input_topic])

        # Confluent Producer 설정
        self.producer = Producer({
            'bootstrap.servers': self.bootstrap_servers,
            'enable.idempotence': True  # 멱등성 보장
        })

    def run(self):
        logger.info("Food-AI Worker (Stable Mode) 시작됨...")
        try:
            while True:
                # poll() 방식을 사용하여 FD 에러 방지 및 안정적인 수신
                msg = self.consumer.poll(1.0)
                if msg is None:
                    continue
                if msg.error():
                    # 파티션 끝 도달 (정상적인 대기 상태)
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    # 토픽이 아직 생성되지 않았을 때 죽지 않고 버티기!
                    elif msg.error().code() == KafkaError.UNKNOWN_TOPIC_OR_PART:
                        logger.warning(f"토픽이 아직 생성되지 않았습니다. Spring Boot의 첫 요청을 대기합니다... ({msg.error()})")
                        time.sleep(1)  # 2초 대기 후 다시 poll 시도
                        continue
                    # 그 외 진짜 치명적인 에러일 때만 종료
                    else:
                        logger.error(f"치명적 Kafka Error: {msg.error()}")
                        break

                self.process_message(msg)
        finally:
            self.consumer.close()
            logger.info("Food-AI Worker가 안전하게 종료되었습니다.")

    def process_message(self, msg):
        start_time = time.time()

        # [Observability] Header 추출 (confluent-kafka 스펙에 맞춤)
        headers = msg.headers()
        trace_id = "no-trace-id"
        if headers:
            for key, value in headers:
                if key == 'X-Trace-Id':
                    trace_id = value.decode('utf-8')
                    break

        # 데이터 파싱
        data = json.loads(msg.value().decode('utf-8'))
        s3_key = data.get('s3_key')
        user_id = data.get('userId')

        logger.info(f"[{trace_id}] 분석 요청 수신: {s3_key}")

        tmp = tempfile.NamedTemporaryFile(suffix=".jpg", delete=False)
        temp_path = tmp.name
        tmp.close()

        try:
            self.s3_client.download_file(self.bucket_name, s3_key, temp_path)
            results = self.model.predict(source=temp_path, save=False, conf=0.421)
            result_obj = results[0]
            work_id = str(uuid.uuid4())

            detected_foods = []
            s3_pred_key = f"temp/results/preds/{work_id}_result.jpg"

            if result_obj.boxes:
                annotated_img = result_obj.plot()
                self._upload_image_to_s3(annotated_img, s3_pred_key)

                for i, box in enumerate(result_obj.boxes):
                    class_id = int(box.cls)
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    crop_img = result_obj.orig_img[y1:y2, x1:x2]
                    crop_key = f"temp/results/crops/{work_id}_{i}.jpg"
                    self._upload_image_to_s3(crop_img, crop_key)

                    detected_foods.append({
                        "class_id": class_id,
                        "class_name": self.model.names[class_id],
                        "crop_img_key": crop_key
                    })

            result_payload = {
                "res_code": "1",
                "userId": user_id,
                "diet_img_pred": s3_pred_key,
                "detected_foods": detected_foods
            }
            # 결과 전송
            self.producer.produce(
                self.result_topic,
                value=json.dumps(result_payload).encode('utf-8'),
                headers=[('X-Trace-Id', trace_id.encode('utf-8'))]
            )
            self.producer.flush()  # 즉시 전송 보장

            elapsed = time.time() - start_time
            logger.info(f"[{trace_id}] 분석 완료 (소요시간: {elapsed:.2f}s)")

        except Exception as e:
            logger.error(f"[{trace_id}] 분석 에러: {str(e)}", exc_info=True)
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

    def _upload_image_to_s3(self, img_array, s3_key):
        tmp = tempfile.NamedTemporaryFile(suffix=".jpg", delete=False)
        tmp_path = tmp.name
        tmp.close()
        try:
            cv2.imwrite(tmp_path, img_array)
            self.s3_client.upload_file(tmp_path, self.bucket_name, s3_key, ExtraArgs={'ContentType': 'image/jpeg'})
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)


if __name__ == "__main__":
    worker = FoodAIWorker()
    worker.run()