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
                <el-select size="small" @change="modelChange(365)" v-model="userHealthQueryDto.healthModelConfigId"
                    placeholder="请选择">
                    <el-option v-for="model in usersHealthModelConfig" :key="model.id" :label="model.name"
                        :value="model.id">
                    </el-option>
                </el-select>
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
            <h2 style="padding-left: 20px;border-left: 2px solid rgb(43, 121, 203);">健康指标数据</h2>
            <el-row style="padding: 10px;margin-left: 10px;">
                <el-row style="display: flex;justify-content: left;align-items: center;gap: 10px;">
                    <el-select size="small" @change="fetchFreshData" v-model="healthModelConfigId" placeholder="请选择模型项">
                        <el-option :key="null" label="全部" :value="null">
                        </el-option>
                        <el-option v-for="model in usersHealthModelConfig" :key="model.id" :label="model.name"
                            :value="model.id">
                        </el-option>
                    </el-select>
                    <el-date-picker size="small" @change="timeChange" style="width: 220px;" v-model="searchTime"
                        type="daterange" range-separator="至" start-placeholder="记录开始" end-placeholder="记录结束">
                    </el-date-picker>
                </el-row>
            </el-row>
            <el-row style="margin: 0 20px;border-top: 1px solid rgb(245,245,245);">
                <el-table row-key="id" @selection-change="handleSelectionChange" :data="tableData">
                    <el-table-column prop="name" label="指标项">
                        <template slot-scope="scope">
                            <span><i class="el-icon-paperclip" style="margin-right: 3px;"></i>{{ scope.row.name
                                }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="value" width="148" label="数值" sortable>
                        <template slot-scope="scope">
                            <span style="font-weight: 800;">{{ scope.row.value }}&nbsp;{{ scope.row.unit }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="symbol" width="108" label="模型符号"></el-table-column>
                    <el-table-column prop="name" width="88" label="状态">
                        <template slot-scope="scope">
                            <i v-if="!statusCheck(scope.row)" style="margin-right: 5px;" class="el-icon-warning"></i>
                            <i v-else style="margin-right: 5px;color: rgb(253, 199, 50);" class="el-icon-success"></i>
                            <el-tooltip v-if="!statusCheck(scope.row)" class="item" effect="dark"
                                content="异常指标，提醒用户及时处理" placement="bottom-end">
                                <span style="text-decoration: underline;text-decoration-style: dashed;">异常</span>
                            </el-tooltip>
                            <span v-else>正常</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="createTime" width="178" label="记录时间" sortable></el-table-column>
                    <el-table-column label="操作" width="80">
                        <template slot-scope="scope">
                            <span class="text-button" @click="handleDelete(scope.row)">删除</span>
                        </template>
                    </el-table-column>
                </el-table>
                <el-pagination style="margin: 20px 0;" @size-change="handleSizeChange"
                    @current-change="handleCurrentChange" :current-page="currentPage" :page-sizes="[10, 20]"
                    :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper"
                    :total="totalItems"></el-pagination>
            </el-row>
        </div>

        <el-dialog title="AI 个人健康档案与趋势分析" :visible.sync="showAnalysisDialog" width="60%" :close-on-click-modal="false">
            <div v-loading="analyzing" element-loading-text="Kimi 正在分析您的健康数据，请稍候...">
                <div v-if="analysisResult" class="markdown-body" v-html="renderedAnalysis" style="line-height: 1.8; padding: 10px;"></div>
                <div v-else-if="!analyzing" style="text-align: center; padding: 40px; color: #909399;">
                    <i class="el-icon-s-data" style="font-size: 48px; margin-bottom: 20px;"></i>
                    <p>点击下方按钮，生成您的专属健康报告</p>
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
        }
    },
    created() {
        this.loadHealthModelConfig();
        this.fetchFreshData();
    },
    methods: {
        timeChange() {
            this.currentPage = 1;
            this.fetchFreshData();
        },
        handleDelete(row) {
            this.selectedRows = [row];
            this.batchDelete();
        },
        /**
         * 状态检查逻辑：
         * 返回 true 表示正常，返回 false 表示异常。
         * 逻辑：只要无法判断（无阈值、非数值），一律视为正常，避免误报。
         */
        statusCheck(data) {
            let { value, valueRange } = data;

            // 1. 无阈值，或阈值字符串为空/null -> 正常
            if (!valueRange || String(valueRange).trim() === '' || valueRange === 'null') {
                return true; 
            }

            // 2. 无输入值 -> 正常
            if (value === null || value === undefined || String(value).trim() === '') {
                return true;
            }

            // 3. 处理中文逗号兼容性
            valueRange = valueRange.replace('，', ',');
            
            // 如果没有逗号分隔符，视为无效阈值 -> 正常
            if (valueRange.indexOf(',') === -1) {
                return true;
            }

            const parts = valueRange.split(',');
            // 使用 parseFloat 解析，如果是文本会变成 NaN
            const min = parseFloat(parts[0]);
            const max = parseFloat(parts[1]);
            const val = parseFloat(value);

            // 4. 关键：非数值检查
            // 如果输入的是文字（如"良好"），或者阈值不是数字 -> 正常
            if (isNaN(val) || isNaN(min) || isNaN(max)) {
                return true;
            }

            // 5. 只有当三者都是有效数字时，才进行范围判定 (包含边界值)
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
                            title: '删除提示',
                            text: response.data.msg,
                            icon: 'success',
                            showConfirmButton: false,
                            timer: 2000,
                        });
                        this.fetchFreshData();
                        return;
                    }
                } catch (e) {
                    this.$swal.fire({
                        title: '错误提示',
                        text: e,
                        icon: 'error',
                        showConfirmButton: false,
                        timer: 2000,
                    });
                    console.error(`用户健康记录信息删除异常：`, e);
                }
            }
        },
        handleFilter() {
            this.currentPage = 1;
            this.fetchFreshData();
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
                console.error('查询用户健康记录信息异常:', error);
            }
        },
        handleFilterClear() {
            this.filterText = '';
            this.handleFilter();
        },
        handleSelectionChange(selection) {
            this.selectedRows = selection;
        },
        resetQueryCondition() {
            this.searchTime = [];
            this.healthModelConfigId = null;
            this.fetchFreshData();
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
        modelUserChange() {
            this.fetchFreshData();
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
            this.analysisResult = ''; 
            try {
                // 添加 { timeout: 300000 } 参数，设置超时为 5 分钟
                // 确保 AI 有足够的时间思考和生成长报告
                const response = await this.$axios.get('/kimi/analyze', {
                    timeout: 300000 
                });
                console.log("后端返回的完整数据：", response.data);

                const { data } = response;
                if (data.code === 200) {
                    // 【检查】这里 data.data 到底有没有值？
                    if (!data.data) {
                        console.error("出问题了：code是200，但 data 字段是空的！");
                        this.$message.warning("分析成功，但没有收到内容，请检查后端返回值");
                    } else {
                        this.analysisResult = data.data;
                        this.$message.success("分析报告生成成功！");
                    }
                } else {
                    this.$message.error(data.msg || '分析失败');
                }
            } catch (error) {
                console.error('智能分析请求失败:', error);
                if (error.code === 'ECONNABORTED') {
                    this.$message.error('分析时间较长，请求超时，请检查网络或稍后重试');
                } else {
                    this.$message.error('智能分析服务暂时不可用');
                }
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

.status-success {
    display: inline-block;
    padding: 1px 5px;
    border-radius: 2px;
    background-color: rgb(201, 237, 249);
    color: rgb(111, 106, 196);
    font-size: 12px;
}

.status-error {
    display: inline-block;
    padding: 1px 5px;
    border-radius: 2px;
    background-color: rgb(233, 226, 134);
    color: rgb(131, 138, 142);
    color: rgb(111, 106, 196);
    font-size: 12px;
}
</style>