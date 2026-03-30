import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
	// 가상 유저 300명이 동시에 달려들어 10초간 계속 요청을 보냄 -> 10000명으로 변경 후
	// vus: 300,
	vus: 10000,
	duration: '10s',
};

export default function () {
	// 테스트 1: 블로킹 API 테스트할 때는 아래 주석을 풀고 쏘세요. 에러 발생시 localhost, 127.0.0.1 대신 현재 사용 중인 내부 IP나 외부 IP로 설정 필요.
	// http.get('http://192.168.0.8:8082/api/test/blocking');

	// 테스트 2: 논블로킹 API 테스트할 때는 아래 주석을 풀고 쏘세요. 에러 발생시 localhost, 127.0.0.1 대신 현재 사용 중인 내부 IP나 외부 IP로 설정 필요.
	http.get('http://192.168.0.8:8082/api/test/non-blocking');

	sleep(1);
}