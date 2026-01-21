<template>
    <div>
        <div style="padding: 0 50px;">
            <div>
                <p style="font-size: 24px;padding: 10px 0;font-weight: bolder;">
                    <span @click="goBack" style="cursor: pointer;;display: inline-block;padding: 0 20px 0 0;">
                        <i class="el-icon-arrow-left"></i>
                        返回首页
                    </span>
                    健康记录
                </p>
            </div>
        </div>
        <div style="height: 6px;background-color: rgb(248, 248, 248);"></div>
        <div style="padding: 10px 20px;">
            <el-row>
                <el-col :span="6" style="border-right: 1px solid #f1f1f1;min-height: calc(100vh - 250px);">
                    <el-tabs v-model="activeName" @tab-click="handleClick" style="margin-right: 40px;">
                        <el-tab-pane label="全局模型" name="first"></el-tab-pane>
                        <el-tab-pane label="我的模型" name="second"></el-tab-pane>
                    </el-tabs>
                    <div style="padding: 20px 0 30px 0;">
                        <span @click="addModel"
                            style="cursor: pointer;;padding: 10px 20px;background-color: #000;border-radius: 5px;color: #fff;">
                            新增模型
                            <i class="el-icon-right"></i>
                        </span>
                    </div>
                    <div style="display: flex;justify-content: left;align-items: center;">
                        <span>配置名</span>
                        <el-input style="width: 100px;" v-model="userHealthModel.name" placeholder="输入处" clearable
                            @clear="handleFilterClear">
                        </el-input>
                        <el-button class="customer" style="background-color: rgb(43, 121, 203);border: none;"
                            type="primary" @click="searModel">搜索</el-button>
                    </div>
                    <div
                        style="padding: 10px 6px;margin-right: 40px;height: 500px;overflow-y: scroll;overflow-x: hidden;">
                        <div @click="modelSelected(model)" class="item-model" v-for="(model, index) in modelList"
                            :key="index">
                            <el-tooltip class="item" effect="dark" :content="'该项配置【' + model.name + '】，点击即可选中'"
                                placement="bottom">
                                <el-row style="padding: 20px 0;">
                                    <el-col :span="24">
                                        <div style="padding: 0 10px;">
                                            <div style="font-size: 24px;font-weight: bolder; display: flex; align-items: center;">
                                                {{ model.name }}
                                                <el-tag v-if="model.tag" size="mini" effect="dark"
                                                    :style="{ 
                                                        backgroundColor: getTagColor(model.tag), 
                                                        border: 'none', 
                                                        marginLeft: '8px',
                                                        borderRadius: '4px',
                                                        height: '20px',
                                                        lineHeight: '20px',
                                                        padding: '0 5px'
                                                    }">
                                                    {{ model.tag }}
                                                </el-tag>
                                            </div>
                                            <div style="font-size: 14px;margin-top: 5px;">
                                                <span>{{ model.unit }}</span>
                                                <span style="margin-left: 10px;">{{ model.symbol }}</span>
                                                <span @click.stop="updateModel(model)" v-if="!model.isGlobal"
                                                    style="margin-left: 10px;color: #333;cursor: pointer;">修改</span>
                                                <span @click.stop="deleteModel(model)" v-if="!model.isGlobal"
                                                    style="margin-left: 10px;color: red;cursor: pointer;">删除</span>
                                            </div>
                                        </div>
                                    </el-col>
                                </el-row>
                            </el-tooltip>
                        </div>
                    </div>
                </el-col>

                <el-col :span="18">
                    <div style="padding: 0 150px;box-sizing: border-box;">
                        <div
                            style="padding: 15px 0;font-size:24px; display: flex; align-items: center; justify-content: space-between;">
                            <span>数据录入面板
                                <span @click="clearData"
                                    style="font-size: 14px;margin-left: 20px;cursor: pointer;color: #999;">重置</span>
                            </span>

                            <div style="display: flex; gap: 10px;">
                                <el-button size="small" type="text" @click="downloadTemplate">下载模板</el-button>
                                <el-upload 
                                    action="" 
                                    :http-request="customUpload"
                                    :show-file-list="false" 
                                    accept=".xlsx, .xls, .jpg, .jpeg, .png, .pdf">
                                    <el-button size="small" type="primary" icon="el-icon-camera">智能导入 / OCR</el-button>
                                </el-upload>
                            </div>
                        </div>

                        <el-row>
                            <el-row v-if="selectedModel.length === 0">
                                <el-empty description="请从左侧选择模型，或使用智能导入"></el-empty>
                            </el-row>
                            <el-row :gutter="20">
                                <el-col :span="12" v-for="(model, index) in selectedModel" :key="index" style="margin-bottom: 20px;">
                                    <div style="margin-bottom: 8px; font-weight: bold; display: flex; align-items: center;">
                                        {{ model.name }} ({{ model.unit }})
                                        <el-tag v-if="model.tag" size="mini" effect="plain"
                                            :style="{ 
                                                color: getTagColor(model.tag),
                                                borderColor: getTagColor(model.tag),
                                                marginLeft: '8px'
                                            }">
                                            {{ model.tag }}
                                        </el-tag>
                                    </div>
                                    <input type="text" v-model="model.value" class="input-model"
                                        :placeholder="'正常值范围：' + (model.valueRange || '无')">
                                </el-col>
                            </el-row>
                        </el-row>

                    </div>
                    <div style="padding: 50px 150px;">
                        <span @click="toRecord"
                            style="cursor: pointer;padding: 10px 20px;background-color: #000;border-radius: 5px;color: #fff;">
                            立即记录
                            <i class="el-icon-right"></i>
                        </span>

                        <span @mousedown="startRecording" @mouseup="stopRecording" style="cursor: pointer;
                            padding: 10px 20px;background-color: #409EFF;border-radius: 5px;color: #fff;margin-left: 20px;user-select: none;">
                            <i class="el-icon-microphone"></i> {{ isRecording ? '松开结束' : '按住说话' }}
                        </span>
                    </div>
                </el-col>
            </el-row>
        </div>

        <el-dialog :show-close="false" :visible.sync="dialogUserOperaion" width="26%">
            <div slot="title">
                <p class="dialog-title">{{ !isOperation ? '健康模型新增' : '健康模型修改' }}</p>
            </div>
            <div style="padding:0 20px;">
                <el-row style="padding: 0 10px 0 0;">
                    <p><span class="modelName">*配置名</span></p>
                    <input class="input-title" v-model="data.name" placeholder="请输入">
                </el-row>
                <el-row style="padding: 0 10px 0 0;">
                    <p><span class="modelName">标签 (可选)</span></p>
                    <input class="input-title" v-model="data.tag" placeholder="例如: 血常规">
                </el-row>
                <el-row style="padding: 0 10px 0 0;">
                    <p><span class="modelName">*单位</span></p>
                    <input class="input-title" v-model="data.unit" placeholder="请输入">
                </el-row>
                <el-row style="padding: 0 10px 0 0;">
                    <p><span class="modelName">*符号</span></p>
                    <input class="input-title" v-model="data.symbol" placeholder="请输入">
                </el-row>
                <el-row style="padding: 0 20px 0 0;">
                    <p><span class="modelName">*阈值（格式：最小值,最大值）</span></p>
                    <input class="input-title" v-model="data.valueRange" placeholder="请输入">
                </el-row>
                <el-row style="padding: 0 10px 0 0;">
                    <p><span class="modelName">*简介</span></p>
                    <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 3 }" placeholder="简介"
                        v-model="data.detail">
                    </el-input>
                </el-row>
            </div>
            <span slot="footer" class="dialog-footer">
                <el-button size="small" v-if="!isOperation" style="background-color: rgb(43, 121, 203);border: none;"
                    class="customer" type="info" @click="addOperation">新增</el-button>
                <el-button size="small" v-else style="background-color: rgb(43, 121, 203);border: none;"
                    class="customer" type="info" @click="updateOperation">修改</el-button>
                <el-button class="customer" size="small" style="background-color: rgb(241, 241, 241);border: none;"
                    @click="cannel()">取消</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import Logo from '@/components/Logo';
import Recorder from 'recorder-core'
import 'recorder-core/src/engine/mp3'
import 'recorder-core/src/engine/mp3-engine'

export default {
    components: { Logo },
    data() {
        return {
            data: {},
            userInfo: {},
            modelList: [],
            activeName: 'first',
            userHealthModel: { isGlobal: true },
            dialogUserOperaion: false,
            isOperation: false,
            userId: null,
            selectedModel: [], 
            isRecording: false,
            rec: null
        };
    },
    created() {
        this.getUserInfo();
        this.getAllModelConfig();
        this.getUser();
    },
    methods: {
        // 获取标签颜色
        getTagColor(tag) {
            if (!tag) return '#999';
            const colors = {
                '血常规': '#409EFF', // 蓝
                '肝功能': '#67C23A', // 绿
                '肾功能': '#E6A23C', // 黄
                '血脂': '#F56C6C',   // 红
                '血糖': '#909399',   // 灰
                '尿常规': '#FF9900', // 橙
                '甲状腺': '#8E44AD', // 紫
                '电解质': '#16A085', // 青
                '凝血功能': '#D35400', // 深橙
                '炎症指标': '#C0392B', // 深红
                '肿瘤标志物': '#2C3E50', // 深蓝灰
                '血气分析': '#8E44AD'  // 紫

            };
            for (const key in colors) {
                if (tag.includes(key)) return colors[key];
            }
            return '#333'; // 默认黑色
        },
        //语音识别功能实现
        // 1. 开始录音
        startRecording() {
            this.rec = Recorder({
                type: "mp3",
                sampleRate: 16000,
                bitRate: 16
            });

            this.rec.open(() => {
                this.rec.start();
                this.isRecording = true;
                this.$message.success('正在聆听，请说话...（例：血压120）');
            }, (msg, isUserNotAllow) => {
                this.$message.error((isUserNotAllow ? "请开启麦克风权限" : "无法录音:") + msg);
            });
        },

        // 2. 结束录音并上传
        stopRecording() {
            if (!this.rec) return;
            this.rec.stop((blob, duration) => {
                this.isRecording = false;
                this.rec.close(); // 释放资源
                
                // 上传录音文件
                this.uploadVoice(blob);
            }, (msg) => {
                this.$message.error("录音失败:" + msg);
            });
        },

        // 3. 上传并识别
        async uploadVoice(blob) {
            const loading = this.$loading({ text: '正在识别语音...', spinner: 'el-icon-loading' });
            const formData = new FormData();
            // 注意：文件名后缀要和后端 VoiceFormat 一致
            formData.append("file", blob, "voice.mp3");

            try {
                // 调用刚才写的后端接口
                const res = await this.$axios.post('/voice/recognize', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });
                
                loading.close();

                if (res.data.code === 200) {
                    const text = res.data.data;
                    this.$notify({ title: '识别结果', message: text, type: 'success' });
                    
                    // 4. 复用 OCR 的解析逻辑填充数据！
                    // 构造成数组格式以适配 fillDataFromOCR
                    this.fillDataFromOCR([text]); 

                    // 5. (可选) 自动入库
                    // 如果匹配到了数据，可以尝试自动保存，或者让用户确认后再点击“立即记录”
                    setTimeout(() => {
                        if (this.selectedModel.length > 0 && this.selectedModel.some(m => m.value)) {
                            this.$confirm(`识别到数据：${text}，是否立即保存？`, '提示', {
                                confirmButtonText: '保存',
                                cancelButtonText: '稍后',
                                type: 'success'
                            }).then(() => {
                                this.toRecord(); // 调用保存方法
                            }).catch(() => {});
                        }
                    }, 500);

                } else {
                    this.$message.error(res.data.msg || '识别失败');
                }
            } catch (e) {
                loading.close();
                console.error(e);
                this.$message.error('语音服务异常');
            }
        },

        async customUpload(param) {
            const file = param.file;
            const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls');
            const isImageOrPdf = /\.(jpg|jpeg|png|pdf)$/i.test(file.name);

            const formData = new FormData();
            formData.append('file', file);

            // 1. Excel 导入
            if (isExcel) {
                try {
                    const response = await this.$axios.post('/user-health/import', formData, {
                        headers: { 'Content-Type': 'multipart/form-data' }
                    });
                    if (response.data.code === 200) {
                        this.$swal.fire({
                            title: '导入成功',
                            text: '健康记录已批量导入',
                            icon: 'success',
                            timer: 1500
                        });
                        setTimeout(() => { this.$router.push('/user'); }, 2000);
                    } else {
                        this.$message.error(response.data.msg || '导入失败');
                    }
                } catch (err) {
                    this.$message.error('网络异常，导入失败');
                }
                return;
            }

            // 2. OCR 识别
            if (isImageOrPdf) {
                const loading = this.$loading({
                    lock: true,
                    text: '正在进行智能识别与入库，请稍候...',
                    spinner: 'el-icon-loading',
                    background: 'rgba(0, 0, 0, 0.7)'
                });

                try {
                    const response = await this.$axios.post('/ocr/recognition', formData, {
                        headers: { 'Content-Type': 'multipart/form-data' }
                    });
                    
                    loading.close();
                    
                    if (response.data.code === 200) {
                        const resultData = response.data.data;

                        // 核心修复：如果是字符串，直接弹窗
                        if (typeof resultData === 'string') {
                            this.$alert(resultData, '处理完成', {
                                confirmButtonText: '去查看',
                                type: 'success',
                                dangerouslyUseHTMLString: false, 
                                callback: action => {
                                    this.$router.push('/user'); 
                                }
                            });
                            return; 
                        }

                        // 兼容旧逻辑
                        const items = resultData.structured_items || resultData.items || resultData;
                        this.fillDataFromOCR(items);

                    } else {
                        this.$message.error(response.data.msg || '识别失败');
                    }
                } catch (err) {
                    loading.close();
                    console.error(err);
                    this.$message.error('智能识别服务异常，请检查后端服务');
                }
                return;
            }

            this.$message.warning('不支持的文件格式，请上传 Excel 或 图片');
        },

        fillDataFromOCR(items) {
            if (typeof items === 'string') return; 

            if (!items || items.length === 0) {
                this.$message.warning('未能识别到有效文字信息');
                return;
            }

            let matchCount = 0;
            const textLines = items.map(item => {
                return typeof item === 'string' ? item : (item.text || item.name || '');
            });

            this.modelList.forEach(model => {
                const line = textLines.find(text => text.includes(model.name));
                if (line) {
                    const numMatch = line.match(/(\d+(\.\d+)?)/);
                    if (numMatch) {
                        this.addAndFillModel(model, numMatch[0]);
                        matchCount++;
                    }
                }
            });

            if (matchCount > 0) {
                this.$notify({
                    title: '识别完成',
                    message: `已自动匹配并填充 ${matchCount} 项数据，请核对。`,
                    type: 'success',
                    duration: 3000
                });
            } else {
                this.$message.warning('OCR 识别成功，但未能自动匹配到任何已知的健康指标，请手动录入。');
            }
        },

        addAndFillModel(model, value) {
            let selected = this.selectedModel.find(s => s.id === model.id);
            if (!selected) {
                selected = JSON.parse(JSON.stringify(model));
                this.selectedModel.push(selected);
            }
            this.$set(selected, 'value', value);
        },

        downloadTemplate() {
            this.$axios.get('/user-health/template', {
                responseType: 'blob'
            }).then(response => {
                const blobData = response.data ? response.data : response;
                const blob = new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
                const link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = '健康记录导入模板.xlsx';
                link.click();
                window.URL.revokeObjectURL(link.href);
            }).catch(error => {
                console.error(error);
                this.$message.error('模板下载失败');
            });
        },

        async clearData() {
            const confirmed = await this.$swalConfirm({
                title: "重置数据？",
                text: `重置之后需要重新输入,是否继续`,
                icon: 'warning',
            });
            if (confirmed) {
                this.selectedModel = [];
            }
        },
        cannel() {
            this.data = {};
            this.dialogUserOperaion = false;
            this.isOperation = false;
        },
        updateOperation() {
            this.$axios.put('/health-model-config/update', this.data).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.dialogUserOperaion = false;
                    this.isOperation = false;
                    this.data = {};
                    this.$swal.fire({
                        title: '模型修改',
                        text: '模型修改成功',
                        icon: 'success',
                        showConfirmButton: false,
                        timer: 1000,
                    });
                    this.getAllModelConfig();
                }
            })
        },
        updateModel(model) {
            this.data = model;
            this.dialogUserOperaion = true;
            this.isOperation = true;
        },
        async deleteModel(model) {
            const confirmed = await this.$swalConfirm({
                title: '删除模型【' + model.name + "】",
                text: `删除后不可恢复，是否继续？`,
                icon: 'warning',
            });
            if (confirmed) {
                const ids = [];
                ids.push(model.id);
                this.$axios.post('/health-model-config/batchDelete', ids).then(response => {
                    const { data } = response;
                    if (data.code === 200) {
                        this.$swal.fire({
                            title: '模型删除',
                            text: '模型删除成功',
                            icon: 'success',
                            showConfirmButton: false,
                            timer: 1000,
                        });
                        this.getAllModelConfig();
                        this.selectedModel = this.selectedModel.filter(entity => entity.id !== model.id);
                    }
                })
            }
        },
        goBack() {
            this.$router.push('/user');
        },
        toRecord() {
            const userHealths = this.selectedModel.map(entity => {
                return {
                    healthModelConfigId: entity.id,
                    value: entity.value
                }
            });
            if (userHealths.length === 0) {
                this.$message.warning("请至少录入一项数据");
                return;
            }
            this.$axios.post('/user-health/save', userHealths).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.$notify({
                        duration: 1000,
                        title: '记录操作',
                        message: '记录成功',
                        type: 'success'
                    });
                    setTimeout(() => {
                        this.$router.push('/user');
                    }, 2000)
                }
            })
        },
        modelSelected(model) {
            const saveFlag = this.selectedModel.find(entity => entity.id === model.id);
            if (!saveFlag) {
                this.selectedModel.push(JSON.parse(JSON.stringify(model))); 
            }
        },
        searModel() {
            this.getAllModelConfig();
        },
        handleFilterClear() {
            this.userHealthModel.name = '';
            this.getAllModelConfig();
        },
        getUser() {
            const userInfo = sessionStorage.getItem('userInfo');
            if (userInfo) {
                const entity = JSON.parse(userInfo);
                this.userId = entity.id;
            }
        },
        async addOperation() {
            try {
                this.data.userId = this.userId;
                const response = await this.$axios.post('/health-model-config/save', this.data);
              
                if (response.data.code === 200) {
                    this.dialogUserOperaion = false;
                    this.getAllModelConfig();
                    this.data = {};
                    this.$notify.info({
                        duration: 1000,
                        title: '模型新增',
                        message: '新增成功'
                    });
                } else {
                    this.$notify.info({
                        duration: 1000,
                        title: '模型新增',
                        message: response.data.msg
                    });
                }
            } catch (error) {
                console.error('出错:', error);
                this.$message.error('提交失败');
            }
        },
        addModel() {
            this.dialogUserOperaion = true;
        },
        handleClick(tab, event) {
            this.userHealthModel = {};
            if (this.activeName === 'first') {
                this.userHealthModel.isGlobal = true;
            } else {
                const userInfo = sessionStorage.getItem('userInfo');
                const entity = JSON.parse(userInfo);
                this.userHealthModel.userId = entity.id;
            }
            this.getAllModelConfig();
        },
        getAllModelConfig() {
            this.$axios.post('/health-model-config/query', this.userHealthModel).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.modelList = data.data;
                }
            });
        },
        getUserInfo() {
            const userInfo = sessionStorage.getItem('userInfo');
            if (userInfo) {
                this.userInfo = JSON.parse(userInfo);
            }
        },
    },
};
</script>
<style scoped lang="scss">
.item-model:hover {
    cursor: pointer;
    background-color: #fafafa;
    border-radius: 5px;
}

.item-model {
    padding: 8px;
    box-sizing: border-box;
}

.input-model {
    font-size: 20px;
    box-sizing: border-box;
    font-weight: bold;
    padding: 20px;
    user-select: none;
    border-radius: 5px;
    border: none;
    outline: none;
    background-color: #f1f1f1;
    height: 50px;
    width: 85%;
}
</style>