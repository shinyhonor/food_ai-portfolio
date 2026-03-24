<template>
  <nav class="navbar navbar-expand-lg navbar-light sticky-top" style="top:0px">
    <router-link to="/" class="navbar-brand d-flex align-items-center px-4 px-lg-5">
      <img class="navimg" src="../../assets/img/samplelogo.png" style="width: 70px; height: 70px; top: 0; left: 10px;" />
    </router-link>
    <button type="button" class="navbar-toggler me-4" data-bs-toggle="collapse" data-bs-target="#navbarCollapse">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarCollapse">
      <div class="navbar-nav ms-auto p-4 p-lg-0">
      </div>
    </div>
  </nav>
</template>

<script>
export default {
name: 'Navbar',
    methods:{
    }
};
</script>
/* 특정 디바이스 사이즈에 대한 스타일 */
<style scoped>  
@media screen and (max-width: 1080px) and (max-height: 2220px) {
  .navbar{
    height: 70px;
  }
  .navimg{
    width: 50px !important; /* !important를 사용하여 다른 스타일이 덮어쓰지 않도록 */
    height: 50px !important;
    margin-top: 10px;
    margin-bottom: 65px;
  }
  .navbar-toggler{
    margin-top: 10px;
    margin-bottom: 60px;
  }
}

</style>