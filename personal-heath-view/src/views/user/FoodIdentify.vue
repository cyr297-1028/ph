<template>
  <div class="food-identify-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="box-card entry-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-edit-outline"></i> 饮食打卡 (Diet Entry)</span>
          </div>
          
          <el-tabs v-model="activeTab">
            <el-tab-pane label="AI 拍照识餐 (多图)" name="ai">
              <div class="upload-section">
                <el-upload
                  class="upload-demo"
                  drag
                  action=""
                  :multiple="true"
                  :auto-upload="false"
                  :on-change="handleChange"
                  :file-list="fileList"
                  accept="image/jpeg,image/png">
                  <i class="el-icon-upload"></i>
                  <div class="el-upload__text">将多张食物照片拖到此处，或<em>点击添加</em></div>
                </el-upload>
                <div style="margin-top: 15px; text-align: center;">
                  <el-button type="primary" icon="el-icon-magic-stick" @click="submitMeal" :disabled="fileList.length === 0">
                    一键智能分析并打卡
                  </el-button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="手动录入" name="manual">
              <el-form :model="manualForm" :rules="rules" ref="manualForm" label-width="100px" class="manual-form">
                <el-form-item label="食物名称" prop="foodName">
                  <el-input v-model="manualForm.foodName" placeholder="例如：全麦面包"></el-input>
                </el-form-item>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="热量(kcal)" prop="calories">
                      <el-input v-model="manualForm.calories" type="number" placeholder="0"></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="碳水(g)">
                      <el-input v-model="manualForm.carbs" type="number" placeholder="0"></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="12">
                    <el-form-item label="蛋白质(g)">
                      <el-input v-model="manualForm.protein" type="number" placeholder="0"></el-input>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="脂肪(g)">
                      <el-input v-model="manualForm.fat" type="number" placeholder="0"></el-input>
                    </el-form-item>
                  </el-col>
                </el-row>
                <div style="text-align: center; margin-top: 10px;">
                  <el-button type="success" icon="el-icon-check" @click="submitManual">确认保存记录</el-button>
                  <el-button @click="resetForm">重置</el-button>
                </div>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="box-card chart-card" shadow="hover">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-pie-chart"></i> 今日摄入分析</span>
            <span style="float: right; color: #F56C6C; font-weight: bold;">
              总计: {{ totalMacros.calories }} kcal
            </span>
          </div>
          <div id="nutritionChart" style="width: 100%; height: 280px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card class="box-card" shadow="never">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-s-order"></i> 今日饮食流水账</span>
            <el-button style="float: right; padding: 3px 0" type="text" icon="el-icon-refresh" @click="fetchTodayRecords">刷新数据</el-button>
          </div>
          <el-table :data="todayRecords" border stripe style="width: 100%" max-height="300">
            <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
            <el-table-column prop="foodName" label="食物名称" min-width="150"></el-table-column>
            <el-table-column prop="calories" label="热量 (kcal)" width="120" align="center">
              <template slot-scope="scope">
                <el-tag type="danger" size="medium">{{ scope.row.calories }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="carbs" label="碳水 (g)" width="100" align="center"></el-table-column>
            <el-table-column prop="protein" label="蛋白质 (g)" width="100" align="center"></el-table-column>
            <el-table-column prop="fat" label="脂肪 (g)" width="100" align="center"></el-table-column>
            <el-table-column prop="createTime" label="打卡时间" width="180" align="center">
              <template slot-scope="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { request } from "@/utils/request";
import * as echarts from 'echarts';

export default {
  name: "FoodIdentify",
  data() {
    return {
      activeTab: 'ai',
      fileList: [],
      todayRecords: [], // 表格数据
      chartInstance: null,
      manualForm: { foodName: '', calories: '', carbs: '', protein: '', fat: '' },
      rules: {
        foodName: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
        calories: [{ required: true, message: '请输入大致热量', trigger: 'blur' }]
      }
    };
  },
  computed: {
    // 动态计算图表需要的数据
    totalMacros() {
      let sum = { calories: 0, carbs: 0, protein: 0, fat: 0 };
      this.todayRecords.forEach(item => {
        sum.calories += parseFloat(item.calories || 0);
        sum.carbs += parseFloat(item.carbs || 0);
        sum.protein += parseFloat(item.protein || 0);
        sum.fat += parseFloat(item.fat || 0);
      });
      // 保留一位小数
      return {
        calories: sum.calories.toFixed(1),
        carbs: sum.carbs.toFixed(1),
        protein: sum.protein.toFixed(1),
        fat: sum.fat.toFixed(1)
      };
    }
  },
  mounted() {
    this.fetchTodayRecords(); // 页面加载时获取今日数据
    window.addEventListener("resize", this.resizeChart); // 响应式
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resizeChart);
    if (this.chartInstance) this.chartInstance.dispose();
  },
  methods: {
    // 格式化时间显示
    formatTime(timeStr) {
      if (!timeStr) return '';
      return timeStr.replace('T', ' ').substring(0, 16);
    },

    // 1. 获取今日打卡记录
    async fetchTodayRecords() {
      try {
        const res = await request({ url: '/food/todayList', method: 'get' });
        if (res.code === 200) {
          this.todayRecords = res.data || [];
          this.renderChart(); // 数据获取后更新图表
        }
      } catch (error) {
        console.error("🔥详细报错信息:", error);
          if (error.response) {
            this.$message.error(`保存失败：服务器拒绝访问 (状态码 ${error.response.status})`);
        } else {
            this.$message.error(`保存失败：网络或代码异常 (${error.message})`);
        }
      }
    },

    // 2. 渲染 ECharts 柱状图
    renderChart() {
      if (!this.chartInstance) {
        this.chartInstance = echarts.init(document.getElementById('nutritionChart'));
      }
      const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['碳水化合物', '蛋白质', '脂肪'], axisTick: { alignWithLabel: true } },
        yAxis: { type: 'value', name: '克 (g)' },
        series: [{
          name: '摄入量',
          type: 'bar',
          barWidth: '40%',
          data: [
            { value: this.totalMacros.carbs, itemStyle: { color: '#E6A23C' } },
            { value: this.totalMacros.protein, itemStyle: { color: '#67C23A' } },
            { value: this.totalMacros.fat, itemStyle: { color: '#909399' } }
          ],
          label: { show: true, position: 'top' }
        }]
      };
      this.chartInstance.setOption(option);
    },
    resizeChart() {
      if (this.chartInstance) this.chartInstance.resize();
    },

    // 3. AI 识别提交 (多图)
    handleChange(file, fileList) {
      this.fileList = fileList;
    },
    async submitMeal() {
      const formData = new FormData();
      this.fileList.forEach(file => formData.append('files', file.raw));

      const loading = this.$loading({ lock: true, text: 'AI正在分析营养...', spinner: 'el-icon-loading' });
      try {
        const res = await request({ url: '/food/identifyMeal', method: 'post', data: formData });
        if (res.code === 200) {
          this.$swal.success("打卡成功！");
          this.fileList = []; // 清空上传列表
          this.fetchTodayRecords(); // 刷新表格和图表
        } else {
          this.$message.error(res.msg);
        }
      } catch (error) {
        this.$message.error("请求超时或服务异常");
      } finally {
        loading.close();
      }
    },

    // 4. 手动表单提交
    submitManual() {
      this.$refs.manualForm.validate(async (valid) => {
        if (valid) {
          try {
            const res = await request({ url: '/food/manualAdd', method: 'post', data: this.manualForm });
            if (res.code === 200) {
              this.$message.success("手动录入成功");
              this.resetForm();
              this.fetchTodayRecords(); // 刷新表格和图表
            } else {
              this.$message.error(res.msg);
            }
          } catch (error) {
            this.$message.error("保存失败");
          }
        }
      });
    },
    resetForm() {
      this.$refs.manualForm.resetFields();
      this.manualForm = { foodName: '', calories: '', carbs: '', protein: '', fat: '' };
    }
  }
};
</script>

<style scoped lang="scss">
.food-identify-container {
  padding: 20px;
  
  /* 1. 将固定高度调大到 520px，使图表和上传区域有更充足的空间 */
  .entry-card, .chart-card {
    height: 520px; 
    display: flex;
    flex-direction: column;
  }

  /* 2. 深度穿透：让卡片的内容区域（除去头部）占满剩余空间，并允许垂直滚动 */
  ::v-deep .el-card__body {
    flex: 1;
    overflow-y: auto; 
    padding-bottom: 20px;
  }

  .upload-section {
    padding-top: 10px;
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
  }

  .upload-demo {
    width: 100%;
    text-align: center;
  }

  /* 3. 关键修复：给上传的图片列表加上独立的滚动条和最大高度 */
  ::v-deep .el-upload-list {
    max-height: 160px; /* 超过这个高度就会出现局部滑动框 */
    overflow-y: auto;
    width: 80%;
    margin: 15px auto 0;
    border-top: 1px dashed #ebeef5;
    padding-top: 10px;
  }

  .manual-form {
    padding-right: 20px;
    margin-top: 10px;
  }
}
</style>