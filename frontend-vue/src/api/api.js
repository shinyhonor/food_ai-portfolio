import axios from 'axios';
import Swal from 'sweetalert2';

let accessToken = null;

// 💡 외부에서 토큰을 설정하거나 가져올 수 있도록 캡슐화
export const setAccessToken = (token) => {
    accessToken = token;
};

export const getAccessToken = () => accessToken;

/**
 * 💡 [추가] JWT Access Token의 Payload를 디코딩하여 유저 ID(sub) 추출
 * 10년 차 팁: 서버에 묻지 않고 클라이언트 단에서 즉시 유저 정보를 확인할 때 사용
 */
export const getUserIdFromToken = () => {
    if (!accessToken) return null;
    try {
        const base64Payload = accessToken.split('.')[1];
        const payload = JSON.parse(atob(base64Payload));
        return payload.sub; // JWT 표준 sub 필드
    } catch (e) {
        console.error("Token Parsing Error", e);
        return null;
    }
};

const api = axios.create({
    baseURL: 'http://localhost:8082',
    withCredentials: true
});

// [Request Interceptor] 모든 요청 헤더에 AT 주입
api.interceptors.request.use(config => {
    console.log("🚀 현재 메모리 상의 AccessToken:", accessToken); // 💡 여기서 확인
    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
});

// [Response Interceptor] 401 에러(만료) 시 자동 갱신 로직
api.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;

        // 🔍 [디버깅 로그] 서버가 실제로 준 응답을 확인합니다.
        console.log("❌ 에러 발생 응답 데이터:", error.response?.data);
        console.log("❌ 에러 타입:", error.response?.data?.error_type);

        // 💡 서버가 보낸 EXPIRED 에러 타입 확인
        if (error.response && error.response.status === 401 && 
            error.response.data.error_type === 'EXPIRED' && !originalRequest._retry) {
            
            originalRequest._retry = true;
            console.log("🔄 [RTR] 토큰 만료 감지, 재발급 시도...");

            try {
                // 리프레시 요청은 인터셉터가 없는 순수 axios 사용 (무한루프 방지)
                const res = await axios.post('http://localhost:8082/api/auth/refresh', {}, { withCredentials: true });
                
                accessToken = res.data.accessToken; 
                console.log("✅ [RTR] 토큰 재발급 성공");

                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return api(originalRequest); 
            } catch (refreshError) {
                console.error("🚨 [RTR] 리프레시 토큰도 만료되었습니다.");
                accessToken = null;
                Swal.fire('세션 만료', '다시 로그인해 주세요.', 'warning').then(() => {
                    location.reload(); 
                });
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
);

export default api;