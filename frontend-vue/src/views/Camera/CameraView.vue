<template>
  <div class="page-wrapper">
    <!-- 로딩 스피너 (currentStep이 ANALYZING일 때 표시) -->
    <div v-if="isLoadingImg" class="loading-container">
      <div class="loading-content">
        <ring-loader :loading="true" :color="'#ffffff'" :size="'150px'"></ring-loader>
        <h4 class="mt-4 text-white">AI가 식단을 정밀 분석하고 있습니다...</h4>
      </div>
    </div>

    <!-- 영양소 상세 모달 (중앙 배치 애니메이션) -->
    <Transition name="modal-fade">
      <div v-if="isSelectFoods && currentStep === 'RESULT'" class="nutrition-modal-overlay" @click.self="isSelectFoods = false">
        <div class="nutrition-modal-card shadow-lg">
          <div class="modal-header">
            <h4 class="fw-bold mb-0"><i class="fas fa-chart-pie me-2 text-primary"></i>{{ name }} 영양 리포트</h4>
            <button class="btn-close" @click="isSelectFoods = false"></button>
          </div>
          <div class="modal-body">
            <div class="nt-grid">
              <div class="nt-item highlight"><span>중량</span><strong>{{ weight }}g</strong></div>
              <div class="nt-item highlight"><span>칼로리</span><strong>{{ cal }}kcal</strong></div>
              <div class="nt-item"><span>탄수화물</span><strong>{{ carbo }}g</strong></div>
              <div class="nt-item"><span>단백질</span><strong>{{ protein }}g</strong></div>
              <div class="nt-item"><span>지방</span><strong>{{ fat }}g</strong></div>
              <div class="nt-item"><span>당류</span><strong>{{ sugars }}g</strong></div>
              <div class="nt-item"><span>나트륨</span><strong>{{ sodium }}mg</strong></div>
              <div class="nt-item"><span>콜레스테롤</span><strong>{{ cholesterol }}mg</strong></div>
              <div class="nt-item"><span>칼슘</span><strong>{{ calcium }}mg</strong></div>
              <div class="nt-item"><span>인</span><strong>{{ phosphorus }}mg</strong></div>
              <div class="nt-item"><span>칼륨</span><strong>{{ potassium }}mg</strong></div>
              <div class="nt-item"><span>마그네슘</span><strong>{{ magnesium }}mg</strong></div>
              <div class="nt-item"><span>철</span><strong>{{ iron }}mg</strong></div>
              <div class="nt-item"><span>아연</span><strong>{{ zinc }}mg</strong></div>
              <div class="nt-item"><span>트랜스지방</span><strong>{{ transfat }}g</strong></div>
              <div class="nt-item meal-tag"><span>식사구분</span><strong>{{ mealtime || '-' }}</strong></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-primary w-100 py-2 fw-bold" @click="isSelectFoods = false">확인</button>
          </div>
        </div>
      </div>
    </Transition>

    <Navbar />

    <!-- Page Header -->
    <div class="container-fluid page-header py-4 mb-2 wow fadeIn" data-wow-delay="0.1s">
      <div class="container text-center py-3">
        <h3 class="display-5 text-white mb-2 animated slideInDown">식단 분석 서비스</h3>
      </div>
    </div>

    <div class="container-xxl py-5">
      <div class="container mt-5">
        <div class="row g-5 align-items-stretch">
          
          <!-- 좌측 설정 및 가이드 (고정) -->
          <div class="col-lg-3 col-md-5">
            <div class="white-card guide-section border-start border-4 border-primary ps-4 shadow-sm">
              <img class="img-fluid mb-3" src="../../assets/img/camera/icons8-caution-sign-64.png" style="width: 50px;">
              <h4 class="mb-3">사용 가이드</h4>
              <ul class="guide-list">
                <li>이미지를 <b>촬영</b>하거나 <b>드롭</b>하세요.</li>
                <li>결과 확인 후 <b>음식 버튼</b>을 클릭하세요.</li>
                <li><b>저장</b> 버튼으로 기록을 영구 보관하세요.</li>
              </ul>
            </div>
          </div>

          <!-- 중앙 메인 작업 영역 (상태 기반 렌더링) -->
          <div class="col-lg-6 col-md-7 text-center">
            <div class="white-card main-workspace shadow-sm">
              
              <!-- [STEP: INIT] - 웹캠 또는 드롭존 -->
              <div v-if="currentStep === 'INIT'">
                <div class="media-frame mb-4">
                  <div v-show="isCameraOpen" class="video-wrap">
                    <video ref="camera" autoplay class="rounded"></video>
                    <!-- 셔터 버튼 -->
                    <div class="shutter-overlay">
                      <button class="stylish-shutter" @click="captureWebcam">
                        <i class="fas fa-camera"></i>
                      </button>
                    </div>
                  </div>
                  <div v-if="!isCameraOpen" class="placeholder-box">
                    <i class="fas fa-camera fa-4x opacity-25"></i>
                    <p class="mt-3 text-muted">카메라를 켜거나 이미지를 드롭하세요</p>
                  </div>
                </div>
                <div class="d-flex justify-content-center mb-4">
                  <button @click="toggleCamera" class="btn btn-lg px-5 py-2 btn-pill" :class="isCameraOpen ? 'btn-outline-danger' : 'btn-primary'">
                    {{ isCameraOpen ? '카메라 종료' : '웹캠 촬영 모드' }}
                  </button>
                </div>
                <div class="dropzone-box" @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false" @drop.prevent="onFileDrop" :class="{ 'dragging': isDragging }" @click="$refs.fileInput.click()">
                  <input type="file" ref="fileInput" @change="onFileChange" hidden accept="image/*">
                  <img src="../../assets/img/camera/icons8-upload.gif" width="40" class="mb-2">
                  <p class="mb-0">이미지 파일을 이곳에 드롭하거나 <b>클릭</b>하세요</p>
                </div>
              </div>

              <!-- [STEP: PREVIEW] - 분석 전 미리보기 -->
              <div v-if="currentStep === 'PREVIEW'">
                <h4 class="mb-3">미리보기</h4>
                <div class="media-frame mb-4">
                  <img :src="imagePreview" class="img-fluid rounded">
                </div>
                <div class="d-flex justify-content-center gap-3">
                  <button @click="resetState" class="btn btn-lg btn-outline-secondary px-4">다시 선택</button>
                  <button @click="startAnalysis" class="btn btn-lg btn-primary px-5 fw-bold">분석하기</button>
                </div>
              </div>

              <!-- [STEP: NO_RESULT] - 탐지 실패 -->
              <div v-if="currentStep === 'NO_RESULT'">
                <div class="py-5">
                  <i class="fas fa-search-minus fa-5x text-warning mb-4"></i>
                  <h3 class="fw-bold">음식을 찾지 못했습니다</h3>
                  <p class="text-muted">다른 사진으로 다시 시도해 주세요.</p>
                  <button @click="resetState" class="btn btn-lg btn-primary mt-4 px-5">다시 시도하기</button>
                </div>
              </div>

              <!-- [STEP: RESULT] - 분석 결과 표시 -->
              <div v-if="currentStep === 'RESULT'">
                <h4 class="text-primary mb-3 fw-bold">AI 분석 완료</h4>
                <div class="media-frame mb-4">
                  <img :src="displayPredImg" class="img-fluid rounded border border-3 border-white shadow-sm">
                </div>
                
                <!-- 음식 버튼 리스트 -->
                <div class="food-token-row d-flex justify-content-center flex-wrap gap-3 mb-4">
                  <template v-for="(info, index) in displayFoodInfo" :key="index">
                    <button class="clean-food-btn" @click="selectFood(index)" :class="{ 'active': activeFoodIdx === index }">
                      <img :src="info.displayImg">
                      <span class="name">{{ info.name }}</span>
                    </button>
                  </template>
                </div>

                <div class="total-cal-banner mb-4">
                  <div class="label"><h4><strong>전체 식단 총 칼로리</strong></h4></div>
                  <div class="value"><strong>{{ totalCal }}</strong> <span>kcal</span></div>
                </div>

                <div class="d-flex justify-content-center gap-3">
                  <button @click="resetState" class="btn btn-lg btn-outline-secondary px-4">Retry</button>
                  <button @click="saveToDB" class="btn btn-lg btn-success px-5 fw-bold">식단 저장하기</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 우측 정보창 -->
          <div class="col-lg-3 d-none d-lg-block">
             <div class="white-card status-widget shadow-sm">
                <h5 class="fw-bold mb-4 border-bottom pb-2">시스템 현황</h5>
                <div class="status-row"><span class="dot online"></span>BFF (Spring Boot)</div>
                <div class="status-row"><span class="dot online"></span>AI Worker (Daemon)</div>
                <div class="status-row"><span class="dot online"></span>MinIO Storage</div>
                <hr class="my-4">
                <h5 class="fw-bold mb-3">인증 정보</h5>
                <div class="user-info" v-if="isLoggedIn">
                  <i class="fas fa-user-check text-primary me-2"></i>
                  <strong>{{ userId }}</strong>
                  <div class="mt-2 text-xs text-muted">JWT 인증 유지 중</div>
                </div>
             </div>
          </div>

        </div>
      </div>
    </div>
    
    <canvas ref="canvas" style="display: none;" width="640" height="480"></canvas>
    <Footer />
  </div>
</template>

<script>
import Navbar from '@/components/Navbar/Navbar.vue';
import Footer from '../../components/Footer/Footer.vue';
import RingLoader from 'vue-spinner/src/RingLoader.vue';
import Swal from 'sweetalert2';
import api, { setAccessToken, getAccessToken, getUserIdFromToken } from '@/api/api.js';

export default {
    components: { Navbar, Footer, RingLoader },
    data() {
        return {
            currentStep: 'INIT',
            isCameraOpen: false,
            isDragging: false,
            isLoadingImg: false,
            isLoggedIn: false,
            uploadFile: null,
            imagePreview: null,
            dietS3Key: null,
            sourceType: 'webcam',
            foodInfo: [],
            totalCal: 0,
            displayFoodInfo: [],
            displayPredImg: null,
            activeFoodIdx: null,
            isSelectFoods: false,
            // 16종 영양소
            name: '', weight: 0, cal: 0, carbo: 0, sugars: 0, fat: 0, protein: 0,
            sodium: 0, cholesterol: 0, calcium: 0, phosphorus: 0, potassium: 0, 
            magnesium: 0, iron: 0, zinc: 0, transfat: 0, mealtime: '',
            color: '#ffffff', // 링 로더 색상을 흰색으로 변경
            userId: ''
        };
    },
    mounted() { this.showLoginModal(); },
    methods: {
        resetState() {
            if (this.imagePreview && this.sourceType === 'upload') URL.revokeObjectURL(this.imagePreview);
            this.currentStep = 'INIT';
            this.uploadFile = null;
            this.imagePreview = null;
            this.dietS3Key = null;
            this.isSelectFoods = false;
            this.activeFoodIdx = null;
            this.isLoadingImg = false;
            this.foodInfo = [];
            this.displayFoodInfo = [];
            if (this.isCameraOpen) this.closeCamera();
        },
        async handleLogin(userId, password) {
            try {
                const res = await api.post("/api/auth/login", new URLSearchParams({ userId, password }));
                if (res.status === 200) {
                    this.isLoggedIn = true;
                    setAccessToken(res.data.accessToken);
                    this.userId = getUserIdFromToken();
                    this.initSseConnection();
                    Swal.fire({ icon: 'success', title: '인증 성공', text: `${this.userId}님, 환영합니다.`, timer: 1500, showConfirmButton: false });
                }
            } catch (e) {
                Swal.fire('로그인 실패', '계정 정보를 다시 확인해 주세요.', 'error').then(this.showLoginModal);
            }
        },
        initSseConnection() {
            const token = getAccessToken();
            const eventSource = new EventSource(`http://localhost:8082/api/notifications/subscribe?token=${token}`);
            eventSource.addEventListener("analysis-result", async (e) => {
                const data = JSON.parse(e.data);
                if (data.res_code === "1") {
                    if (!data.foodInfo || data.foodInfo.length === 0) {
                        this.currentStep = 'NO_RESULT';
                        this.isLoadingImg = false;
                        return;
                    }
                    const allKeys = [data.diet_img_pred, ...data.foodInfo.map(i => i.img)];
                    const secureUrls = await this.fetchImageUrlsWithTickets(allKeys);
                    if (secureUrls) {
                        this.displayPredImg = secureUrls[data.diet_img_pred];
                        this.displayFoodInfo = data.foodInfo.map(i => ({ ...i, displayImg: secureUrls[i.img] }));
                        this.foodInfo = data.foodInfo;
                        this.totalCal = data.totalCal;
                        this.currentStep = 'RESULT';
                        this.isLoadingImg = false;
                    }
                }
            });
            eventSource.onerror = () => { eventSource.close(); setTimeout(() => this.initSseConnection(), 5000); };
        },
        onFileDrop(e) {
            this.isDragging = false;
            const file = e.dataTransfer.files[0];
            if (file) this.processFile(file);
        },
        onFileChange(e) {
            const file = e.target.files[0];
            if (file) this.processFile(file);
        },
        processFile(file) {
            this.sourceType = 'upload';
            this.uploadFile = file;
            this.imagePreview = URL.createObjectURL(file);
            this.currentStep = 'PREVIEW';
        },
        toggleCamera() { this.isCameraOpen ? this.closeCamera() : this.openCamera(); },
        async openCamera() {
            try {
                this.$refs.camera.srcObject = await navigator.mediaDevices.getUserMedia({ video: { width: 1280, height: 720 } });
                this.isCameraOpen = true;
            } catch (e) { Swal.fire('에러', '카메라 권한을 확인해주세요.', 'error'); }
        },
        closeCamera() {
            if (this.$refs.camera?.srcObject) this.$refs.camera.srcObject.getTracks().forEach(t => t.stop());
            this.isCameraOpen = false;
        },
        captureWebcam() {
            const ctx = this.$refs.canvas.getContext('2d');
            ctx.drawImage(this.$refs.camera, 0, 0, 640, 480);
            this.imagePreview = this.$refs.canvas.toDataURL("image/jpeg");
            this.sourceType = 'webcam';
            this.currentStep = 'PREVIEW';
            this.closeCamera();
        },
        async startAnalysis() {
            this.isLoadingImg = true;
            try {
                const fd = new FormData();
                let endpoint = "/api/food/detect/webcam";
                if (this.sourceType === 'webcam') fd.append("org_img", this.imagePreview);
                else {
                    fd.append("mfile", this.uploadFile);
                    endpoint = "/api/food/detect/upload";
                }
                const res = await api.post(endpoint, fd);
                this.dietS3Key = res.data.diet_s3_key;
            } catch (e) {
                this.resetState();
                Swal.fire('실패', '서버 통신 중 에러가 발생했습니다.', 'error');
            }
        },
        async saveToDB() {
            try {
                const query = new URLSearchParams({
                    meal_time: (new Date()).toString().slice(16, 21),
                    diet_s3_key: this.dietS3Key,
                    type: this.sourceType
                });
                const res = await api.post(`/api/food/save?${query}`, this.foodInfo);
                if (res.data.res_code === "1") {
                    await Swal.fire('저장 성공', '식단 기록이 안전하게 저장되었습니다.', 'success');
                    this.resetState();
                }
            } catch (e) { Swal.fire('에러', '저장 중 오류가 발생했습니다.', 'error'); }
        },
        async fetchImageUrlsWithTickets(keys) {
            try {
                const res = await api.post("/api/images/ticket/bulk", keys);
                const map = {};
                for (let k in res.data) map[k] = `http://localhost/images/${k}?token=${res.data[k]}`;
                return map;
            } catch (e) { return null; }
        },
        selectFood(idx) {
            this.activeFoodIdx = idx;
            const target = this.displayFoodInfo[idx];
            Object.assign(this, target);
            this.isSelectFoods = true;
        },
        async showLoginModal() {
            const { value: v } = await Swal.fire({
                title: '로그인\n<div class="fs-6 text-muted">테스트(ID:test, PW: test01)</div>',
                html: `
                    <input id="swal-input1" class="swal2-input" placeholder="아이디를 입력하세요" style="margin-top: 5px;margin-bottom: 5px;">
                    <input id="swal-input2" type="password" class="swal2-input" placeholder="비밀번호를 입력하세요" style="margin-top: 5px;margin-bottom: 15px;">
                `,
                preConfirm: () => ({
                    u: document.getElementById('swal-input1').value,
                    p: document.getElementById('swal-input2').value
                }),
                allowOutsideClick: false
            });
            if (v) this.handleLogin(v.u, v.p);
        }
    }
};
</script>

<style scoped>
/* 화이트 스타일 */
.page-wrapper { background-color: #f8fafc; min-height: 100vh; font-family: 'Pretendard', sans-serif; }
.white-card { background: white; border-radius: 16px; padding: 25px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }

/* 미디어 프레임(이미지 잘림 원천 차단) */
.media-frame {
  width: 100%; max-width: 550px; margin: 0 auto;
  aspect-ratio: 4 / 3; background: #000; border-radius: 12px;
  overflow: hidden; display: flex; align-items: center; justify-content: center;
  position: relative;
}
.media-frame video, .media-frame img { 
  width: 100%; height: 100%; 
  object-fit: contain; /* 원본 비율 유지, 절대 안 잘림!!! */
}

/* 셔터 버튼 디자인 */
.shutter-overlay { position: absolute; bottom: 20px; width: 100%; display: flex; justify-content: center; }
.stylish-shutter {
  width: 70px; height: 70px; background: rgba(255,255,255,0.3);
  border: 4px solid white; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: 0.2s;
}
.stylish-shutter i { color: white; font-size: 24px; text-shadow: 0 2px 4px rgba(0,0,0,0.3); }
.stylish-shutter:hover { transform: scale(1.1); background: rgba(255,255,255,0.5); }
.stylish-shutter:active { transform: scale(0.9); }

/* 드롭존 */
.dropzone-box {
  border: 2px dashed #0050FF; border-radius: 12px; padding: 40px;
  background: #f8fbff; cursor: pointer; transition: 0.2s;
}
.dropzone-box.dragging { background: #eef4ff; border-style: solid; }

/* 음식 버튼 */
.clean-food-btn {
  border: 1px solid #eee; background: white; padding: 10px; border-radius: 16px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.06); transition: 0.3s; width: 110px;
}
.clean-food-btn:hover { transform: translateY(-8px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.clean-food-btn.active { border-color: #0050FF; border-width: 2px; }
.clean-food-btn img { width: 70px; height: 70px; border-radius: 50%; object-fit: cover; margin-bottom: 8px; }
.clean-food-btn .name { display: block; font-size: 13px; font-weight: bold; color: #334155; }

/* 영양소 정보 보드 */
.nt-grid-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.nt-item { 
  background: #fdfdfd; padding: 12px 20px; border-radius: 8px; border: 1px solid #f0f0f0;
  display: flex; justify-content: space-between; align-items: center; 
}
.nt-item span { color: #8898aa; font-weight: 500; font-size: 0.9rem; }
.nt-item strong { color: #32325d; font-weight: 800; font-size: 1.05rem; }
.nt-item.highlight { border-left: 4px solid #0050FF; }
.nt-item.highlight strong { color: #0050FF; }

/* 요약 배너 */
.total-cal-banner {
  background: linear-gradient(135deg, #f6f9fc 0%, #eef2f7 100%);
  padding: 25px; border-radius: 12px; display: inline-block; min-width: 280px;
}
.total-cal-banner .label { color: #6366f1; font-weight: 700; font-size: 14px; margin-bottom: 5px; text-transform: uppercase; }
.total-cal-banner .value { font-size: 32px; font-weight: 900; color: #1e293b; }
.total-cal-banner .value span { font-size: 18px; color: #94a3b8; }

/* 시스템 현황 */
.status-row { display: flex; align-items: center; font-size: 14px; margin-bottom: 12px; color: #4b5563; }
.dot { width: 8px; height: 8px; border-radius: 50%; margin-right: 12px; }
.dot.online { background: #22c55e; box-shadow: 0 0 8px rgba(34,197,94,0.5); }
.user-info { background: #f1f5f9; padding: 12px; border-radius: 12px; display: inline-block; width: 100%; }

.loading-container {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 80, 255, 0.8); z-index: 10000;
  display: flex; align-items: center; justify-content: center;
}
.btn-pill { border-radius: 50px; }

/* 애니메이션 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

/* 영양소 모달 스타일 (중앙 팝업) */
.nutrition-modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.6); z-index: 11000;
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.nutrition-modal-card { 
  background: white; border-radius: 24px; width: 100%; max-width: 600px;
  max-height: 90vh; overflow-y: auto; position: relative;
}
.modal-header { padding: 20px 30px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
.nt-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 30px; }
.nt-item { 
  background: #f8fafc; padding: 15px 20px; border-radius: 12px; border: 1px solid #e2e8f0;
  display: flex; justify-content: space-between; align-items: center; 
}
.nt-item span { color: #64748b; font-weight: 600; font-size: 1rem; }
.nt-item strong { color: #1e293b; font-weight: 800; font-size: 1.2rem; }
.nt-item.highlight { border-left: 5px solid #0050FF; background: #f0f7ff; }
.nt-item.highlight strong { color: #0050FF; }
</style>