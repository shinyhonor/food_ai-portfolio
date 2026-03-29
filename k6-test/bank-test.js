import http from 'k6/http';

export const options = {
  vus: 100,        // 100명이 동시에 달려듦
  iterations: 100  // 딱 100번만 요청을 쏘고 종료
};

export default function () {
  // 내부 IP 주소로 통일해서 쏘기. 에러 발생시 localhost, 127.0.0.1 대신 현재 사용 중인 내부 IP나 외부 IP로 설정 필요.
  http.get('http://192.168.0.8:8082/api/bank/withdraw');
}