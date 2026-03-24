from django.urls import path
from food_ai import views

urlpatterns = [
    # path('detectFoodStream', views.detectFoodStream),
    path('detectFoodWeb', views.detectFoodWeb),
    path('detectFoodWeb_save', views.detectFoodWeb_save),

    path('detectFoodWebUpload', views.detectFoodWebUpload),
    path('detectFoodWebUpload_save', views.detectFoodWebUpload_save),

    path('test', views.test),

]
