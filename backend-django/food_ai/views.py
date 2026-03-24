import os
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from config.settings import BASE_DIR
import uuid
import shutil
import base64

import cx_Oracle as ora
from ultralytics import YOLO # Import Ultralytics Package

connection_str = 'food_ai/a1234@localhost/xe'
# Define the video files for the trackers
video_file = 0  # WebCam Path

# Load the YOLOv8 models
model_path = f"{os.path.join(BASE_DIR, 'food_ai', 'static', 'food_ai')}//best_yolov8n.pt"
model = YOLO(model_path) # YOLOv8n Model

# port 설정
myPort = 9000

# 식사시간 알고리즘 상수
base_meal_time = [300, 600, 960, 1260]

#####웹캠 음식 판독#####
@csrf_exempt
def detectFoodWeb(request):
    base_ip = f"http://{request.get_host()}"

    if request.method != 'POST':
        json_ret = {"text": "Invalid request method", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    user_id = request.POST.get('user_id', None)
    if user_id is None:
        json_ret = {"text": "user_id is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)


    conn = ora.connect(connection_str)
    cursor = conn.cursor()
    param = (user_id, )
    sql = 'select member_no from users where user_id = :user_id'
    cursor.execute(sql, param)
    member_no = str(cursor.fetchone()[0])

    ext = '.jpg'
    unique_filename = f"{str(uuid.uuid4())}{ext}"
    unique_filename_pred = f"pred_{unique_filename}"
    base_upload_path = os.path.join(BASE_DIR, 'static', 'upload_web', member_no)
    base_result_path = os.path.join(BASE_DIR, 'static', 'result_web', member_no)
    diet_img = f"{base_upload_path}\\{unique_filename}"
    diet_img_pred = f"{base_upload_path}\\{unique_filename_pred}"
    shutil.rmtree(base_result_path, ignore_errors=True)
    shutil.rmtree(base_upload_path, ignore_errors=True)
    os.makedirs(base_upload_path, exist_ok=True)

    data = request.POST.get('org_img')
    data = data[22:] #앞의 필요없는 부분 제거
    imgdata = base64.b64decode(data)
    with open(diet_img,'wb') as f:
        f.write(imgdata)

    results = model.predict(source=diet_img, save=True, save_crop=True, imgsz=640, project="../static/result_web",name=member_no, conf=0.421, iou=0.5)
    shutil.move(f"{base_result_path}\\{unique_filename}", f"{base_result_path}\\{unique_filename_pred}")

    base_crop_path = f"{base_ip}\\static\\result_web\\{member_no}\\crops"
    ids_txt_path = f"{base_result_path}"
    res_dict = {"foodInfo" : []}
    res_list = []
    totalCal = 0
    for result in results:
        if result.boxes:
            for box in result.boxes:
                class_id = int(box.cls)
                class_name = model.names[class_id]
                res_list.append(f'{class_id}:{class_name}')
                sql = "select * from nutrient where nutrient_id = :1"
                param = (class_id,)
                cursor.execute(sql, param)
                nutrient_all = cursor.fetchall()[0]
                new_dict = {}
                totalCal += nutrient_all[3]
                new_dict["nutrient_id"] = nutrient_all[0]
                new_dict["name"] = nutrient_all[1]
                new_dict["weight"] = nutrient_all[2]
                new_dict["cal"] = nutrient_all[3]
                new_dict["carbo"] = nutrient_all[4]
                new_dict["sugars"] = nutrient_all[5]
                new_dict["fat"] = nutrient_all[6]
                new_dict["protein"] = nutrient_all[7]
                new_dict["calcium"] = nutrient_all[8]
                new_dict["phosphorus"] = nutrient_all[9]
                new_dict["sodium"] = nutrient_all[10]
                new_dict["potassium"] = nutrient_all[11]
                new_dict["magnesium"] = nutrient_all[12]
                new_dict["iron"] = nutrient_all[13]
                new_dict["zinc"] = nutrient_all[14]
                new_dict["cholesterol"] = nutrient_all[15]
                new_dict["transfat"] = nutrient_all[16]
                new_dict["img"] = f"{base_crop_path}\\{class_name}\\{unique_filename}"
                res_dict["foodInfo"].append(new_dict)

    with open(f"{base_result_path}\\ids.txt", 'w') as f:
        f.write(','.join(res_list))

    res_dict["totalCal"] = totalCal
    res_dict["diet_img_pred"] = f"{base_ip}/static/result_web/{member_no}/{unique_filename_pred}"
    res_dict["res_code"] = "1"

    return JsonResponse(data=res_dict, safe=False)

#####웹캠 결과 DB 저장#####
@csrf_exempt
def detectFoodWeb_save(request):
    base_ip = f"http://{request.get_host()}"

    conn = ora.connect(connection_str)
    cursor = conn.cursor()

    #식단 기록
    user_id = request.POST.get('user_id', None)
    if user_id is None:
        json_ret = {"text": "user_id is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    conn = ora.connect(connection_str)
    cursor = conn.cursor()
    param = (user_id, )
    sql = 'select member_no from users where user_id = :user_id'
    cursor.execute(sql, param)
    member_no = str(cursor.fetchone()[0])

    base_upload_path = os.path.join(BASE_DIR, 'static', 'upload_web', member_no)
    base_result_path = os.path.join(BASE_DIR, 'static', 'result_web', member_no)
    base_images_path = os.path.join(BASE_DIR, 'static', 'images', member_no)
    diet_path = os.path.join(base_images_path, 'diet')
    food_path = os.path.join(base_images_path, 'food')

    detect_info_txt_path = os.path.join(base_result_path, 'ids.txt')
    file_stats = os.stat(detect_info_txt_path)
    if file_stats.st_size == 0:
        json_ret = {"text": "이미지에서 음식을 탐지하지 못하였습니다.", "res_code":"0"}
        return JsonResponse(data=json_ret, safe=False)

    org_diet_name = os.listdir(base_upload_path)[0]
    diet_src = f"{base_upload_path}\\{org_diet_name}"
    diet_dest = f"{diet_path}\\{org_diet_name}"
    os.makedirs(diet_path, exist_ok=True)
    shutil.move(diet_src, diet_dest)

    diet_web_path = f"{base_ip}/static/images/{member_no}/diet/{org_diet_name}"

    cur_time = request.POST.get('meal_time', None)
    if cur_time is None:
        json_ret = {"text": "meal_time is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)


    time_info = cur_time.split(":")
    meal_time = timeToMealtime(time_info)

    param = (member_no, diet_web_path, meal_time)
    sql = 'insert into diet values(diet_seq.nextVal, :1, sysdate, :2, :3)'
    cursor.execute(sql, param)

    #음식 기록
    cursor.execute('select diet_seq.currVal from dual')
    diet_id = cursor.fetchone()[0]
    with open(detect_info_txt_path, 'r') as f:
        detect_into_txt = f.readline()
        info_list = detect_into_txt.split(',')
        for i, info in enumerate(info_list):
            info_data = info.split(':')
            food_src = os.path.join(base_result_path, 'crops', info_data[1], org_diet_name)
            food_dest = os.path.join(food_path, f"{i+1}_{org_diet_name}")
            os.makedirs(food_path, exist_ok=True)
            shutil.move(food_src, food_dest)

            food_web_path = f"{base_ip}/static/images/{member_no}/food/{i+1}_{org_diet_name}"
            param = (diet_id, info_data[0], food_web_path)
            sql = 'insert into food_rec values(food_rec_seq.nextVal, :1, :2, :3)'
            cursor.execute(sql, param)

    conn.commit()
    cursor.close()
    conn.close()
    json_ret = {"text": "식단 기록 저장 완료", "res_code": "1"}
    return JsonResponse(data=json_ret, safe=False)

#####웹업로드_음식_판독#####
@csrf_exempt
def detectFoodWebUpload(request):
    base_ip = f"http://{request.get_host()}"

    if request.method != 'POST':
        json_ret = {"text": "Invalid request method", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    user_id = request.POST.get('user_id', None)
    if user_id is None:
        json_ret = {"text": "user_id is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    mfile = request.FILES.get('mfile', None)
    if mfile is None:
        json_ret = {"text": "mfile is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    conn = ora.connect(connection_str)
    cursor = conn.cursor()
    param = (user_id, )
    sql = 'select member_no from users where user_id = :user_id'
    cursor.execute(sql, param)
    member_no = str(cursor.fetchone()[0])

    ext = mfile.name[mfile.name.rindex('.'):]
    unique_filename = f"{str(uuid.uuid4())}{ext}"
    unique_filename_pred = f"pred_{unique_filename}"
    base_upload_path = os.path.join(BASE_DIR, 'static', 'upload_web2', member_no)
    base_result_path = os.path.join(BASE_DIR, 'static', 'result_web2', member_no)
    diet_img = f"{base_upload_path}\\{unique_filename}"
    diet_img_pred = f"{base_upload_path}\\{unique_filename_pred}"
    shutil.rmtree(base_result_path, ignore_errors=True)
    shutil.rmtree(base_upload_path, ignore_errors=True)
    os.makedirs(base_upload_path, exist_ok=True)
    if mfile:
        with open(diet_img, 'wb+') as f:
            for chunk in mfile.chunks():
                f.write(chunk)

    # , imgsz=640
    results = model.predict(source=diet_img, save=True, save_crop=True, project="../static/result_web2",name=member_no, conf=0.421, iou=0.5)
    shutil.move(f"{base_result_path}\\{unique_filename}", f"{base_result_path}\\{unique_filename_pred}")

    base_crop_path = f"{base_ip}\\static\\result_web2\\{member_no}\\crops"
    ids_txt_path = f"{base_result_path}"
    res_dict = {"foodInfo" : []}
    res_list = []
    totalCal = 0
    for result in results:
        if result.boxes:
            for box in result.boxes:
                class_id = int(box.cls)
                class_name = model.names[class_id]
                res_list.append(f'{class_id}:{class_name}')
                sql = "select * from nutrient where nutrient_id = :1"
                param = (class_id,)
                cursor.execute(sql, param)
                nutrient_all = cursor.fetchall()[0]
                new_dict = {}
                totalCal += nutrient_all[3]
                new_dict["nutrient_id"] = nutrient_all[0]
                new_dict["name"] = nutrient_all[1]
                new_dict["weight"] = nutrient_all[2]
                new_dict["cal"] = nutrient_all[3]
                new_dict["carbo"] = nutrient_all[4]
                new_dict["sugars"] = nutrient_all[5]
                new_dict["fat"] = nutrient_all[6]
                new_dict["protein"] = nutrient_all[7]
                new_dict["calcium"] = nutrient_all[8]
                new_dict["phosphorus"] = nutrient_all[9]
                new_dict["sodium"] = nutrient_all[10]
                new_dict["potassium"] = nutrient_all[11]
                new_dict["magnesium"] = nutrient_all[12]
                new_dict["iron"] = nutrient_all[13]
                new_dict["zinc"] = nutrient_all[14]
                new_dict["cholesterol"] = nutrient_all[15]
                new_dict["transfat"] = nutrient_all[16]
                new_dict["img"] = f"{base_crop_path}\\{class_name}\\{unique_filename[:-4]}.jpg"
                res_dict["foodInfo"].append(new_dict)

    with open(f"{base_result_path}\\ids.txt", 'w') as f:
        f.write(','.join(res_list))

    res_dict["totalCal"] = totalCal
    res_dict["diet_img_upload_pred"] = f"{base_ip}/static/result_web2/{member_no}/{unique_filename_pred}"
    res_dict["res_code"] = "1"

    cursor.close()
    conn.close()
    return JsonResponse(data=res_dict, safe=False)

#####웹업로드_결과_DB_저장#####
@csrf_exempt
def detectFoodWebUpload_save(request):
    base_ip = f"http://{request.get_host()}"

    conn = ora.connect(connection_str)
    cursor = conn.cursor()

    #식단 기록
    user_id = request.POST.get('user_id', None)
    if user_id is None:
        json_ret = {"text": "user_id is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    param = (user_id, )
    sql = 'select member_no from users where user_id = :user_id'
    cursor.execute(sql, param)
    member_no = str(cursor.fetchone()[0])

    base_upload_path = os.path.join(BASE_DIR, 'static', 'upload_web2', member_no)
    base_result_path = os.path.join(BASE_DIR, 'static', 'result_web2', member_no)
    base_images_path = os.path.join(BASE_DIR, 'static', 'images', member_no)
    diet_path = os.path.join(base_images_path, 'diet')
    food_path = os.path.join(base_images_path, 'food')

    detect_info_txt_path = os.path.join(base_result_path, 'ids.txt')
    file_stats = os.stat(detect_info_txt_path)
    if file_stats.st_size == 0:
        json_ret = {"text": "이미지에서 음식을 탐지하지 못하였습니다.", "res_code":"0"}
        return JsonResponse(data=json_ret, safe=False)

    org_diet_name = os.listdir(base_upload_path)[0]
    res_diet_name = f"{os.listdir(base_upload_path)[0][:-4]}.jpg"
    diet_src = f"{base_upload_path}\\{org_diet_name}"
    diet_dest = f"{diet_path}\\{org_diet_name}"
    os.makedirs(diet_path, exist_ok=True)
    shutil.move(diet_src, diet_dest)

    diet_web_path = f"{base_ip}/static/images/{member_no}/diet/{org_diet_name}"

    cur_time = request.POST.get('meal_time', None)
    if cur_time is None:
        json_ret = {"text": "meal_time is None", "res_code": "0"}
        return JsonResponse(data=json_ret, safe=False)

    time_info = cur_time.split(":")
    meal_time = timeToMealtime(time_info)

    param = (member_no, diet_web_path, meal_time)
    sql = 'insert into diet values(diet_seq.nextVal, :1, sysdate, :2, :3)'
    cursor.execute(sql, param)

    #음식 기록
    cursor.execute('select diet_seq.currVal from dual')
    diet_id = cursor.fetchone()[0]
    with open(detect_info_txt_path, 'r') as f:
        detect_into_txt = f.readline()
        info_list = detect_into_txt.split(',')
        for i, info in enumerate(info_list):
            info_data = info.split(':')
            food_src = os.path.join(base_result_path, 'crops', info_data[1], res_diet_name)
            food_dest = os.path.join(food_path, f"{i+1}_{res_diet_name}")
            os.makedirs(food_path, exist_ok=True)
            shutil.move(food_src, food_dest)

            food_web_path = f"{base_ip}/static/images/{member_no}/food/{i+1}_{res_diet_name}"
            param = (diet_id, info_data[0], food_web_path)
            sql = 'insert into food_rec values(food_rec_seq.nextVal, :1, :2, :3)'
            cursor.execute(sql, param)

    conn.commit()
    cursor.close()
    conn.close()
    json_ret = {"text": "식단 기록 저장 완료", "res_code": "1"}
    return JsonResponse(data=json_ret, safe=False)


#####connection_test#####
@csrf_exempt
def test(request):
    print("test진입")
    # json_ret = {"id": request.GET.get('id', None)}
    json_ret = {"테스트" : "성공"}
    return JsonResponse(json_ret, safe=False)


#####util_methods#####
def timeToMealtime(time_info):
    cur_time = int(time_info[0]) * 60 + int(time_info[1])
    meal_time = None
    if base_meal_time[0] <= cur_time < base_meal_time[1]:
        meal_time = '조식'
    elif base_meal_time[1] <= cur_time < base_meal_time[2]:
        meal_time = '중식'
    elif base_meal_time[2] <= cur_time < base_meal_time[3]:
        meal_time = '석식'
    else:
        meal_time = '야식'

    return meal_time


