<template>
    <div class="login-container">
        <div class="header">
            <Logo :bag="colorLogo" bag="rgb(51,51,51)" sysName="健康有道" />
        </div>
        <div class="login-panel">
            <div class="left-image">
                <img src="/bag.png" class="health-image" />
                <div class="motivational-text">
                    <p>每天一小步，健康一大步</p>
                    <p>让运动成为习惯</p>
                </div>
            </div>
            <div class="right-login">
                <div class="welcome-section">
                    <h2>欢迎回来！</h2>
                    <p class="subtitle">开启您的健康生活之旅</p>
                </div>
                <div class="input-group">
                    <input v-model="act" class="act" placeholder="输入账号" />
                    <span class="input-icon">👤</span>
                </div>
                <div class="input-group">
                    <input v-model="pwd" class="pwd" type="password" placeholder="输入密码" />
                    <span class="input-icon">🔒</span>
                </div>
                <div>
                    <button class="login-btn" @click="login">立即登录</button>
                </div>
                <div class="tip">
                    <p>还没有账号？<span class="no-act" @click="toDoRegister">立即注册</span></p>
                </div>
            </div>
        </div>
        <div class="footer-motivation">
            <p>生命在于运动 · 健康源于坚持</p>
        </div>
    </div>
</template>

<script>
const DELAY_TIME = 1300;
import request from "@/utils/request.js";
import { setToken } from "@/utils/storage.js";
import md5 from 'js-md5';
import Logo from '@/components/Logo.vue';
export default {
    name: "Login",
    components: { Logo },
    data() {
        return {
            act: '',
            pwd: '',
            colorLogo: 'rgb(38,38,38)',
            isCameraOpen: false, // 控制摄像头弹窗
            mediaStream: null    // 媒体流对象
        }
    },
    methods: {
        // 跳转注册页面
        toDoRegister() {
            // 跳转
            this.$router.push('/register');
        },
        // 打开摄像头
        async openCamera() {
            this.isCameraOpen = true;
            try {
                this.mediaStream = await navigator.mediaDevices.getUserMedia({ video: true });
                this.$nextTick(() => {
                    const video = this.$refs.video;
                    video.srcObject = this.mediaStream;
                });
            } catch (err) {
                console.error("摄像头开启失败:", err);
                this.$swal.fire({
                    title: '错误',
                    text: '无法访问摄像头，请检查权限或设备',
                    icon: 'error'
                });
                this.isCameraOpen = false;
            }
        },
        // 关闭摄像头
        closeCamera() {
            if (this.mediaStream) {
                this.mediaStream.getTracks().forEach(track => track.stop());
                this.mediaStream = null;
            }
            this.isCameraOpen = false;
        },
        // 拍照并执行人脸登录
        captureAndLogin() {
            const video = this.$refs.video;
            const canvas = this.$refs.canvas;
            
            // 设置画布尺寸与视频一致
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            
            const context = canvas.getContext('2d');
            context.drawImage(video, 0, 0, canvas.width, canvas.height);
            
            // 转换为Blob上传
            canvas.toBlob(async (blob) => {
                const formData = new FormData();
                formData.append('file', blob, 'face.png');
                
                try {
                    // 显示加载中
                    this.$swal.showLoading();
                    
                    const { data } = await request.post('/user/faceLogin', formData, {
                        headers: { 'Content-Type': 'multipart/form-data' }
                    });
                    
                    if (data.code === 200) {
                        this.closeCamera(); // 识别成功关闭摄像头
                        
                        this.$swal.fire({
                            title: '登录成功',
                            text: '欢迎回来',
                            icon: 'success',
                            timer: DELAY_TIME,
                            showConfirmButton: false
                        });
                        
                        setToken(data.data.token);
                        setTimeout(() => {
                            const { role } = data.data;
                            sessionStorage.setItem('role', role);
                            this.navigateToRole(role);
                        }, DELAY_TIME);
                    } else {
                        this.$swal.fire('识别失败', data.msg, 'error');
                    }
                } catch (error) {
                    console.error(error);
                    this.$swal.fire('错误', '人脸识别服务异常', 'error');
                }
            }, 'image/png');
        },
        async login() {
            if (!this.act || !this.pwd) {
                this.$swal.fire({
                    title: '填写校验',
                    text: '账号或密码不能为空',
                    icon: 'error',
                    showConfirmButton: false,
                    timer: DELAY_TIME,
                });
                return;
            }
            const hashedPwd = md5(md5(this.pwd));
            const paramDTO = { userAccount: this.act, userPwd: hashedPwd };
            try {
                const { data } = await request.post(`user/login`, paramDTO);
                if (data.code !== 200) {
                    this.$swal.fire({
                        title: '登录失败',
                        text: data.msg,
                        icon: 'error',
                        showConfirmButton: false,
                        timer: DELAY_TIME,
                    });
                    return;
                }
                setToken(data.data.token);
                // 根据角色延迟跳转
                setTimeout(() => {
                    const { role } = data.data;
                    sessionStorage.setItem('role', role);
                    this.navigateToRole(role);
                }, DELAY_TIME);
            } catch (error) {
                console.error('登录请求错误:', error);
                this.$message.error('登录请求出错，请重试！');
            }
        },
        navigateToRole(role) {
            switch (role) {
                case 1:
                    this.$router.push('/admin');
                    break;
                case 2:
                    this.$router.push('/user');
                    break;
                default:
                    console.warn('未知的角色类型:', role);
                    break;
            }
        },
    },
    // 组件销毁前确保关闭摄像头
    beforeDestroy() {
        this.closeCamera();
    }
};
</script>

<style lang="scss" scoped>
* {
    user-select: none;
    box-sizing: border-box;
}

.login-container {
    width: 100%;
    min-height: 100vh;
    background-color: #f8fafc;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: column;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
    position: relative;
    overflow: hidden;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, rgba(74, 194, 154, 0.1) 0%, rgba(67, 176, 242, 0.1) 100%);
        z-index: 0;
    }

    .header {
        display: flex;
        justify-content: left;
        margin: 20px 0;
        width: 100%;
        max-width: 1200px;
        padding: 0 20px;
        z-index: 1;
    }

    .login-panel {
        display: flex;
        justify-content: space-between;
        height: auto;
        border-radius: 16px;
        background: white;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
        overflow: hidden;
        z-index: 1;
        width: 800px;
        max-width: 90%;
        position: relative;

        &::after {
            content: '';
            position: absolute;
            bottom: -10px;
            left: 5%;
            width: 90%;
            height: 10px;
            background: linear-gradient(to right, #4ac29a, #67b0f2);
            border-radius: 0 0 16px 16px;
            filter: blur(10px);
            opacity: 0.6;
        }

        .left-image {
            width: 45%;
            padding: 40px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            background: linear-gradient(to bottom right, #4ac29a, #67b0f2);
            color: white;

            .health-image {
                width: 100%;
                max-width: 160px;
                border-radius: 10px;
                animation: float 3s ease-in-out infinite;
            }

            .motivational-text {
                margin-top: 30px;
                text-align: center;

                p {
                    font-size: 18px;
                    font-weight: 500;
                    margin: 10px 0;
                    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
            }
        }

        .right-login {
            width: 55%;
            padding: 50px;
            display: flex;
            flex-direction: column;
            justify-content: center;

            .welcome-section {
                margin-bottom: 30px;

                h2 {
                    color: #2d3748;
                    font-size: 28px;
                    margin-bottom: 8px;
                    font-weight: 700;
                }

                .subtitle {
                    color: #718096;
                    font-size: 14px;
                    font-weight: 400;
                }
            }

            .input-group {
                position: relative;
                margin: 15px 0;

                .input-icon {
                    position: absolute;
                    left: 15px;
                    top: 50%;
                    transform: translateY(-50%);
                    font-size: 18px;
                    color: #a0aec0;
                }
            }
        }
    }

    .act,
    .pwd {
        height: 50px;
        width: 100%;
        font-size: 16px;
        padding: 0 15px 0 45px;
        background-color: #f8fafc;
        border: 2px solid #e2e8f0;
        border-radius: 8px;
        transition: all 0.3s ease;
        color: #4a5568;
        font-weight: 500;

        &:focus {
            outline: none;
            border-color: #4ac29a;
            box-shadow: 0 0 0 3px rgba(74, 194, 154, 0.2);
        }

        &::placeholder {
            color: #a0aec0;
            font-weight: 400;
        }
    }

    .login-btn {
        display: inline-block;
        text-align: center;
        border-radius: 8px;
        margin-top: 25px;
        height: 50px;
        line-height: 50px;
        width: 100%;
        background: linear-gradient(to right, #4ac29a, #67b0f2);
        font-size: 16px;
        font-weight: 600;
        border: none;
        color: white;
        cursor: pointer;
        transition: all 0.3s ease;
        box-shadow: 0 4px 6px rgba(74, 194, 154, 0.2);

        &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 12px rgba(74, 194, 154, 0.3);
        }

        &:active {
            transform: translateY(0);
        }
    }

    .tip {
        margin: 25px 0 0;
        text-align: center;

        p {
            padding: 3px 0;
            margin: 0;
            font-size: 14px;
            color: #718096;

            .no-act {
                color: #4ac29a;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s ease;

                &:hover {
                    color: #3aa884;
                    text-decoration: underline;
                }
            }
        }
    }

    .footer-motivation {
        margin: 20px 0;
        color: #718096;
        font-size: 14px;
        font-weight: 500;
        z-index: 1;
    }
}

@keyframes float {
    0% {
        transform: translateY(0px);
    }

    50% {
        transform: translateY(-10px);
    }

    100% {
        transform: translateY(0px);
    }
}

@media (max-width: 768px) {
    .login-panel {
        flex-direction: column;

        .left-image,
        .right-login {
            width: 100% !important;
        }

        .left-image {
            padding: 30px !important;

            .health-image {
                max-width: 200px !important;
            }
        }
    }
}

.face-login-btn {
    width: 100%;
    margin-top: 15px;
    height: 45px;
    line-height: 45px;
    background: white;
    border: 2px solid #4ac29a;
    color: #4ac29a;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
        background: #f0f9f6;
    }
    
    .icon {
        margin-right: 5px;
    }
}

.camera-modal {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0,0,0,0.6);
    z-index: 999;
    display: flex;
    justify-content: center;
    align-items: center;

    .camera-content {
        background: white;
        padding: 20px;
        border-radius: 16px;
        text-align: center;
        width: 500px;
        max-width: 90%;
        
        h3 {
            margin-bottom: 15px;
            color: #333;
        }
        
        .video-preview {
            width: 100%;
            border-radius: 8px;
            background: #000;
            margin-bottom: 20px;
        }
        
        .camera-controls {
            display: flex;
            justify-content: center;
            gap: 20px;
            
            button {
                padding: 10px 30px;
                border-radius: 6px;
                border: none;
                cursor: pointer;
                font-weight: 600;
            }
            
            .capture-btn {
                background: #4ac29a;
                color: white;
            }
            
            .cancel-btn {
                background: #e2e8f0;
                color: #4a5568;
            }
        }
    }
}
</style>