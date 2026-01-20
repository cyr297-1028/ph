<template>
    <div>
        <div style="border-radius: 5px;padding: 20px 0 60px 0;width: 100%;background-color: #fafafa;">
            <div style="height: 100px;line-height: 100px;text-align: center;font-size: 24px;">
                健康生活，健康人生，从此刻开始！
            </div>
            <div style="height: 50px;line-height: 50px;text-align: center;font-size: 30px;font-weight: bolder;">
                每一点改变，都值得被记录。
                <span @click="toRecord"
                    style="cursor: pointer;padding: 5px 10px;background-color: #000;border-radius: 5px;color: #fff; font-size: 16px; vertical-align: middle;">
                    前去记录
                    <i class="el-icon-right"></i>
                </span>
                
                <span @click="openAnalysisDialog"
                    style="cursor: pointer;padding: 5px 10px;background-color: #409EFF;border-radius: 5px;color: #fff; margin-left: 15px; font-size: 16px; vertical-align: middle;">
                    <i class="el-icon-data-analysis"></i> 生成健康档案
                </span>
            </div>
        </div>
        
        <div style="padding: 30px 0;">
            <div style="margin: 20px 0;">
                <span style="font-weight: bold; margin-right: 10px; color: #606266;">趋势分析：</span>
                <el-select size="small" @change="modelChange(365)" v-model="userHealthQueryDto.healthModelConfigId"
                    placeholder="请选择健康指标" style="width: 300px;">
                    <el-option v-for="model in usersHealthModelConfig" :key="model.id" :label="model.name" :value="model.id">
                        <span style="float: left">{{ model.name }}</span>
                        <span v-if="model.tag" style="float: right; color: #8492a6; font-size: 12px; margin-left: 15px;">
                            <span :style="{color: getTagColor(model.tag), fontWeight: 'bold'}">● {{ model.tag }}</span>
                        </span>
                    </el-option>
                </el-select>
                <el-tag v-if="currentSelectedTag" size="small" 
                    :style="{ backgroundColor: getTagColor(currentSelectedTag), color: '#fff', border: 'none', marginLeft: '10px' }">
                    {{ currentSelectedTag }}
                </el-tag>
            </div>
            <div>
                <div v-if="values.length === 0">
                    <el-empty description="暂无数据，快去记录吧"></el-empty>
                </div>
                <div v-else>
                    <LineChart @on-selected="onSelectedTime" height="500px" tag="" :values="values" :date="dates" />
                </div>
            </div>
        </div>
        
        <div>
            <h2 style="padding-left: 20px;border-left: 4px solid #409EFF; margin-bottom: 20px;">健康指标数据明细</h2>
            
            <el-row style="padding: 10px;margin-left: 10px; background-color: #fff; border-radius: 4px;">
                <el-row style="display: flex;justify-content: left;align-items: center;gap: 10px;">
                    <el-select size="small" @change="fetchFreshData" v-model="healthModelConfigId" placeholder="按指标筛选" clearable>
                        <el-option v-for="model in usersHealthModelConfig" :key="model.id" :label="model.name" :value="model.id">
                            <span style="float: left">{{ model.name }}</span>
                            <span v-if="model.tag" style="float: right; color: #8492a6; font-size: 12px;">{{ model.tag }}</span>
                        </el-option>
                    </el-select>
                    
                    <el-date-picker size="small" @change="timeChange" style="width: 240px;" v-model="searchTime"
                        type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期">
                    </el-date-picker>
                </el-row>
            </el-row>

            <el-row style="margin: 0 20px; border-top: 1px solid #EBEEF5;">
                <el-table row-key="id" @selection-change="handleSelectionChange" :data="tableData" stripe>
                    
                    <el-table-column prop="name" label="指标项" min-width="160">
                        <template slot-scope="scope">
                            <span style="font-weight: 500;">{{ scope.row.name }}</span>
                            <el-tag v-if="scope.row.tag" size="mini" effect="dark"
                                :style="{ 
                                    backgroundColor: getTagColor(scope.row.tag), 
                                    borderColor: getTagColor(scope.row.tag),
                                    marginLeft: '8px',
                                    borderRadius: '10px',
                                    padding: '0 8px'
                                }">
                                {{ scope.row.tag }}
                            </el-tag>
                        </template>
                    </el-table-column>

                    <el-table-column prop="value" width="150" label="数值" sortable>
                        <template slot-scope="scope">
                            <span style="font-weight: bold; font-size: 15px; color: #303133;">
                                {{ scope.row.value }}
                            </span>
                            <span style="font-size: 12px; color: #909399; margin-left: 4px;">{{ scope.row.unit }}</span>
                        </template>
                    </el-table-column>

                    <el-table-column prop="symbol" width="100" label="符号" align="center">
                        <template slot-scope="scope">
                            <el-tag type="info" size="mini" effect="plain">{{ scope.row.symbol || '-' }}</el-tag>
                        </template>
                    </el-table-column>

                    <el-table-column prop="name" width="100" label="状态" align="center">
                        <template slot-scope="scope">
                            <template v-if="!statusCheck(scope.row)">
                                <el-tag type="danger" size="small" effect="dark">异常</el-tag>
                            </template>
                            <template v-else>
                                <el-tag type="success" size="small" effect="light">正常</el-tag>
                            </template>
                        </template>
                    </el-table-column>

                    <el-table-column prop="createTime" width="180" label="记录时间" sortable align="center" class-name="time-col"></el-table-column>
                    
                    <el-table-column label="操作" width="100" align="center">
                        <template slot-scope="scope">
                            <el-button type="text" style="color: #F56C6C;" icon="el-icon-delete" @click="handleDelete(scope.row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
                
                <el-pagination style="margin: 20px 0; text-align: right;" @size-change="handleSizeChange"
                    @current-change="handleCurrentChange" :current-page="currentPage" :page-sizes="[10, 20, 50]"
                    :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper"
                    :total="totalItems"></el-pagination>
            </el-row>
        </div>

        <el-dialog title="AI 个人健康档案与趋势分析" :visible.sync="showAnalysisDialog" width="60%" :close-on-click-modal="false">
            <div v-loading="analyzing" element-loading-text="Kimi 正在深度思考您的健康数据，请耐心等待 1-2 分钟...">
                <div v-if="analysisResult" 
                     class="markdown-body" 
                     v-html="renderedAnalysis" 
                     style="line-height: 1.8; padding: 10px; max-height: 60vh; overflow-y: auto;">
                </div>
                
                <div v-else-if="!analyzing" style="text-align: center; padding: 40px; color: #909399;">
                    <i class="el-icon-s-data" style="font-size: 48px; margin-bottom: 20px;"></i>
                    <p>点击下方按钮，生成您的专属健康报告</p>
                    <p style="font-size: 12px; color: #ccc;">报告生成后将自动保存至“我的消息”</p>
                </div>
            </div>
            <span slot="footer" class="dialog-footer">
                <el-button @click="showAnalysisDialog = false">关 闭</el-button>
                <el-button type="primary" @click="startAnalysis" :disabled="analyzing" :loading="analyzing">
                    {{ analysisResult ? '重新生成' : '开始分析' }}
                </el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import LineChart from '@/components/LineChart.vue';
import { marked } from 'marked';

export default {
    components: { LineChart },
    data() {
        return {
            usersHealthModelConfig: [],
            modelConfigList: [],
            userHealthQueryDto: {},
            values: [],
            dates: [],
            tableData: [],
            selectedRows: [],
            currentPage: 1,
            pageSize: 20,
            totalItems: 0,
            searchTime: [],
            healthModelConfigId: null,
            showAnalysisDialog: false,
            analyzing: false,
            analysisResult: ''
        }
    },
    computed: {
        renderedAnalysis() {
            if (!this.analysisResult) return '';
            return marked(this.analysisResult);
        },
        // 获取当前选中模型的标签
        currentSelectedTag() {
            if (!this.userHealthQueryDto.healthModelConfigId) return null;
            const model = this.usersHealthModelConfig.find(m => m.id === this.userHealthQueryDto.healthModelConfigId);
            return model ? model.tag : null;
        }
    },
    created() {
        this.loadHealthModelConfig();
        this.fetchFreshData();
    },
    methods: {
        // 【核心算法】根据标签分配颜色
        getTagColor(tag) {
            if (!tag) return '#909399';
            const colors = {
                '血常规': '#409EFF',   // 品牌蓝
                '肝功能': '#67C23A',   // 成功绿
                '肾功能': '#E6A23C',   // 警告黄
                '血脂': '#F56C6C',     // 危险红
                '血糖': '#9B59B6',     // 紫色
                '尿常规': '#FF9F43',   // 橙色
                '甲状腺': '#E91E63',   // 粉色
                '电解质': '#1ABC9C',   // 青色
                '心血管': '#C0392B',   // 深红
                '肿瘤': '#34495E',     // 深灰
                '炎症': '#D35400'      // 南瓜色
            };
            // 模糊匹配
            for (const key in colors) {
                if (tag.includes(key)) return colors[key];
            }
            return '#909399'; // 默认灰
        },
        timeChange() {
            this.currentPage = 1;
            this.fetchFreshData();
        },
        handleDelete(row) {
            this.selectedRows = [row];
            this.batchDelete();
        },
        statusCheck(data) {
            let { value, valueRange } = data;
            if (!valueRange || String(valueRange).trim() === '' || valueRange === 'null') return true; 
            if (value === null || value === undefined || String(value).trim() === '') return true;
            
            // 兼容性处理
            valueRange = valueRange.replace('，', ',');
            valueRange = valueRange.replace(' - ', ',');
            valueRange = valueRange.replace('~', ',');
            
            if (valueRange.indexOf(',') === -1) return true;
            const parts = valueRange.split(',');
            const min = parseFloat(parts[0]);
            const max = parseFloat(parts[1]);
            const val = parseFloat(value);
            if (isNaN(val) || isNaN(min) || isNaN(max)) return true;
            return val >= min && val <= max;
        },
        async batchDelete() {
            if (!this.selectedRows.length) {
                this.$message(`未选中任何数据`);
                return;
            }
            const confirmed = await this.$swalConfirm({
                title: '删除健康记录数据',
                text: `删除后不可恢复，是否继续？`,
                icon: 'warning',
            });
            if (confirmed) {
                try {
                    let ids = this.selectedRows.map(entity => entity.id);
                    const response = await this.$axios.post(`/user-health/batchDelete`, ids);
                    if (response.data.code === 200) {
                        this.$swal.fire({
                            title: '删除成功',
                            text: '数据已删除',
                            icon: 'success',
                            showConfirmButton: false,
                            timer: 1500,
                        });
                        this.fetchFreshData();
                    }
                } catch (e) {
                    console.error(`删除异常：`, e);
                }
            }
        },
        async fetchFreshData() {
            try {
                let startTime = null;
                let endTime = null;
                if (this.searchTime != null && this.searchTime.length === 2) {
                    const [startDate, endDate] = await Promise.all(this.searchTime.map(date => date.toISOString()));
                    startTime = `${startDate.split('T')[0]}T00:00:00`;
                    endTime = `${endDate.split('T')[0]}T23:59:59`;
                }
                const userInfo = sessionStorage.getItem('userInfo');
                const userEntitySave = JSON.parse(userInfo);
                const params = {
                    current: this.currentPage,
                    size: this.pageSize,
                    startTime: startTime,
                    endTime: endTime,
                    healthModelConfigId: this.healthModelConfigId,
                    userId: userEntitySave.id
                };
                const response = await this.$axios.post('/user-health/query', params);
                const { data } = response;
                this.tableData = data.data;
                this.totalItems = data.total;
            } catch (error) {
                console.error('查询异常:', error);
            }
        },
        handleSelectionChange(selection) {
            this.selectedRows = selection;
        },
        handleSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.fetchFreshData();
        },
        handleCurrentChange(val) {
            this.currentPage = val;
            this.fetchFreshData();
        },
        loadUserModelHavaRecord() {
            if (!this.userHealthQueryDto.healthModelConfigId) return;
            this.$axios.get(`/user-health/timeQuery/${this.userHealthQueryDto.healthModelConfigId}/${this.userHealthQueryDto.time}`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.values = data.data.map(entity => entity.value).reverse();
                    this.dates = data.data.map(entity => entity.createTime).reverse();
                }
            })
        },
        modelChange(day) {
            this.onSelectedTime(day);
            this.loadUserModelHavaRecord();
        },
        loadHealthModelConfig() {
            this.$axios.post("/health-model-config/modelList").then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.usersHealthModelConfig = data.data;
                    this.modelConfigList = data.data;
                    this.defaultLoad();
                }
            })
        },
        defaultLoad() {
            if (this.usersHealthModelConfig.length > 0) {
                this.userHealthQueryDto.healthModelConfigId = this.usersHealthModelConfig[0].id;
                this.onSelectedTime(365);
                this.loadUserModelHavaRecord();
            }
        },
        onSelectedTime(time) {
            this.userHealthQueryDto.time = time;
            this.loadUserModelHavaRecord();
        },
        toRecord() {
            this.$router.push('/record');
        },
        openAnalysisDialog() {
            this.showAnalysisDialog = true;
        },
        async startAnalysis() {
            this.analyzing = true;
            this.$message.info('正在请求 AI 分析，请耐心等待...');
            try {
                const response = await this.$axios.get('/kimi/analyze', {
                    responseType: 'blob', 
                    timeout: 300000 
                });
                const blob = new Blob([response.data], { type: 'text/plain;charset=utf-8' });
                const downloadUrl = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = downloadUrl;
                link.setAttribute('download', `健康分析报告_${new Date().getTime()}.txt`);
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                window.URL.revokeObjectURL(downloadUrl);
                this.$message.success('报告已下载');
                this.showAnalysisDialog = false;
            } catch (error) {
                console.error('分析失败:', error);
                this.$message.error('下载报告失败，请稍后重试');
            } finally {
                this.analyzing = false;
            }
        }
    }
};
</script>

<style scoped lang="scss">
.markdown-body ::v-deep {
    h1, h2, h3 { margin-top: 20px; margin-bottom: 10px; font-weight: bold; }
    p { margin-bottom: 10px; }
    ul, ol { padding-left: 20px; margin-bottom: 10px; }
    li { list-style-type: disc; }
    strong { color: #333; font-weight: 700; }
}
.time-col { color: #909399; font-size: 12px; }
</style>