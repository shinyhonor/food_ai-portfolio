import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:80/project/api/v1';

export const getKakaoData = (accessToken) => {
  return axios.get('/auth/oauth2/kakao', {
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });
  
};