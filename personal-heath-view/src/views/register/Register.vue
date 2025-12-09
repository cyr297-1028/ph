<template>
    <div class="register-container">
        <div class="bg-overlay"></div>
        <div class="register-panel">
            <div class="logo-section">
                <Logo sysName="没账号？立即注册" />
                <h2 class="slogan">开启健康活力的生活之旅</h2>
            </div>
            <div class="input-group">
                <i class="fa-solid fa-user"></i>
                <input v-model="act" class="input-field" placeholder="注册账号" />
            </div>
            <div class="input-group">
                <i class="fa-solid fa-signature"></i>
                <input v-model="name" class="input-field" placeholder="用户名" />
            </div>
            <div class="input-group">
                <i class="fa-solid fa-lock"></i>
                <input v-model="pwd" class="input-field" type="password" placeholder="输入密码" />
            </div>
            <div class="input-group">
                <i class="fa-solid fa-lock"></i>
                <input v-model="pwdConfirm" class="input-field" type="password" placeholder="确认密码" />
            </div>
            <div class="button-group">
                <span class="register-button" @click="registerFunc">立即注册</span>
            </div>
            <div class="tip">
                <p>已有账户？<span class="login-link" @click="toDoLogin">返回登录</span></p>
            </div>
        </div>
        <div class="feature-icons">
            <i class="fa-solid fa-dumbbell"></i>
            <i class="fa-solid fa-running"></i>
            <i class="fa-solid fa-biking"></i>
        </div>

        <div v-if="isCameraOpen" class="camera-modal">
            <div class="camera-content">
                <h3>录入人脸数据</h3>
                <p class="hint">请保持光线充足，正脸注视摄像头</p>
                <video ref="video" autoplay playsinline class="video-preview"></video>
                <canvas ref="canvas" style="display: none;"></canvas>
                <div class="camera-controls">
                    <button @click="captureAndUpload" class="capture-btn">录入</button>
                    <button @click="skipFaceUpload" class="cancel-btn">跳过</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
const DELAY_TIME = 1300;
import request from "@/utils/request.js";
import md5 from 'js-md5';
import Logo from '@/components/Logo.vue';

export default {
    name: "Register",
    components: { Logo },
    data() {
        return {
            act: '',
            pwd: '',
            pwdConfirm: '',
            name: '',
            isCameraOpen: false,
            mediaStream: null
        }
    },
    methods: {
        toDoLogin() {
            this.$router.push('/login');
        },
        // 注册逻辑修改
        async registerFunc() {
            if (!this.act || !this.pwd || !this.pwdConfirm || !this.name) {
                this.$swal.fire({ title: '填写校验', text: '请填写完整信息', icon: 'error', timer: DELAY_TIME, showConfirmButton: false });
                return;
            }
            if (this.pwd !== this.pwdConfirm) {
                this.$swal.fire({ title: '填写校验', text: '两次密码不一致', icon: 'error', timer: DELAY_TIME, showConfirmButton: false });
                return;
            }
            const hashedPwd = md5(md5(this.pwd));
            const paramDTO = { userAccount: this.act, userPwd: hashedPwd, userName: this.name };
            try {
                const { data } = await request.post(`user/register`, paramDTO);
                if (data.code !== 200) {
                    this.$swal.fire({ title: '注册失败', text: data.msg, icon: 'error', timer: DELAY_TIME, showConfirmButton: false });
                    return;
                }
                
                // 注册成功，询问是否录入人脸
                this.$swal.fire({
                    title: '注册成功',
                    text: "是否立即录入人脸数据用于刷脸登录？",
                    icon: 'success',
                    showCancelButton: true,
                    confirmButtonColor: '#28a745',
                    cancelButtonColor: '#d33',
                    confirmButtonText: '立即录入',
                    cancelButtonText: '稍后再说'
                }).then((result) => {
                    if (result.isConfirmed) {
                        this.openCamera();
                    } else {
                        this.toDoLogin();
                    }
                });

            } catch (error) {
                console.error('注册请求错误:', error);
            }
        },
        async openCamera() {
            this.isCameraOpen = true;
            try {
                this.mediaStream = await navigator.mediaDevices.getUserMedia({ video: true });
                this.$nextTick(() => {
                    this.$refs.video.srcObject = this.mediaStream;
                });
            } catch (err) {
                this.$swal.fire('错误', '无法打开摄像头', 'error');
                this.isCameraOpen = false;
                this.toDoLogin();
            }
        },
        skipFaceUpload() {
            this.closeCamera();
            this.toDoLogin();
        },
        closeCamera() {
            if (this.mediaStream) {
                this.mediaStream.getTracks().forEach(track => track.stop());
                this.mediaStream = null;
            }
            this.isCameraOpen = false;
        },
        captureAndUpload() {
            const video = this.$refs.video;
            const canvas = this.$refs.canvas;
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const context = canvas.getContext('2d');
            context.drawImage(video, 0, 0, canvas.width, canvas.height);

            canvas.toBlob(async (blob) => {
                const formData = new FormData();
                formData.append('file', blob, 'face_register.png');
                formData.append('userAccount', this.act); // 关键：带上账号

                try {
                    this.$swal.showLoading();
                    const { data } = await request.post('/user/addFace', formData, {
                        headers: { 'Content-Type': 'multipart/form-data' }
                    });

                    if (data.code === 200) {
                        this.$swal.fire({
                            title: '录入成功',
                            text: '现在可以使用刷脸登录了',
                            icon: 'success',
                            timer: 2000,
                            showConfirmButton: false
                        }).then(() => {
                            this.closeCamera();
                            this.toDoLogin();
                        });
                    } else {
                        this.$swal.fire('录入失败', data.msg, 'error');
                    }
                } catch (e) {
                    this.$swal.fire('错误', '网络异常', 'error');
                }
            }, 'image/png');
        }
    },
    beforeDestroy() {
        this.closeCamera();
    }
};
</script>

<style lang="scss" scoped>
// ... 原有样式 ...
// 添加弹窗样式，建议将 camera-modal 样式提取到全局或公共CSS中复用
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
        padding: 25px;
        border-radius: 16px;
        text-align: center;
        width: 500px;
        max-width: 90%;
        
        h3 { margin-bottom: 10px; color: #333; }
        .hint { color: #666; font-size: 14px; margin-bottom: 20px; }
        
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
            .capture-btn { background: #28a745; color: white; }
            .cancel-btn { background: #e2e8f0; color: #4a5568; }
        }
    }
}
</style>