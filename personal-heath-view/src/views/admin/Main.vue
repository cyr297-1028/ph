<template>
    <div class="container-main">
        <el-row>
            <el-col :span="6">
                <div style="padding: 10px 5px;box-sizing: border-box;">
                    <h2 style="margin-top: 0;">最新消息</h2>
                    <div class="timeline-container">
                        <div v-for="(message, index) in messageList" :key="message" class="timeline-item">
                            <div class="receiver-name">{{ message.receiverName }}</div>
                            <div class="message-content">{{ parseText(message.content) }}</div>
                            <div class="create-time">{{ message.createTime }}</div>
                        </div>
                    </div>
                </div>
            </el-col>
            <el-col :span="18">
                <div style="box-sizing: border-box;">
                    <PieChart fontColor="rgb(51,51,51)" tag="信息占比" :values="pieValues" :types="pieTypes" />
                </div>
                <div style="display: flex;justify-content: space-evenly;">
                    <div style="box-sizing: border-box;">
                        <LineChart height="220px" tag="存量用户" @on-selected="userDatesSelected" :values="userValues"
                            :date="userDates" />
                    </div>
                    <div style="box-sizing: border-box;">
                        <LineChart height="220px" tag="健康指标" @on-selected="modelDatesSelected" :values="modelValues"
                            :date="modelDates" />
                    </div>

                </div>

            </el-col>
        </el-row>
    </div>
</template>
<script>
import LineChart from "@/components/LineChart"
import PieChart from "@/components/PieChart"
import { timeAgo } from "@/utils/data"
export default {
    components: { LineChart, PieChart },
    data() {
        return {
            userValues: [],
            userDates: [],
            modelDates: [],
            modelValues: [],
            pieValues: [],
            pieTypes: [],
            messageList: []
        }
    },
    created() {
        // 数据较少，默认查365天
        this.userDatesSelected(365);
        // 数据较少，默认查365天
        this.modelDatesSelected(365);
        this.loadPieCharts();
        this.loadMessages();
    },
    methods: {
        parseText(text) {
            // 使用正则表达式判断文本是否符合由分号分隔且至少有三项的结构
            const pattern = /^([^;]+;){2}[^;]+$/;
            if (pattern.test(text)) {
                // 使用分号分割文本
                const parts = text.split(';');
                // 返回第三项内容
                return parts[2];
            }
            // 若不满足条件则返回原文本
            return text;
        },
        time(createTime) {
            return timeAgo(createTime);
        },
        // 加载资讯
        loadMessages() {
            const messageQueryDto = {
                current: 1,
                size: 4
            }
            this.$axios.post(`/message/query`, messageQueryDto).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.messageList = data.data;
                }
            })
        },
        loadPieCharts() {
            this.$axios.get(`/views/staticControls`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.pieValues = data.data.map(entity => entity.count);
                    this.pieTypes = data.data.map(entity => entity.name);
                }
            })
        },
        modelDatesSelected(time) {
            this.$axios.get(`/user-health/daysQuery/${time}`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.modelValues = data.data.map(entity => entity.count);
                    this.modelDates = data.data.map(entity => entity.name);
                }
            })
        },
        userDatesSelected(time) {
            this.$axios.get(`/user/daysQuery/${time}`).then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.userValues = data.data.map(entity => entity.count);
                    this.userDates = data.data.map(entity => entity.name);
                }
            })
        },
    },
};
</script>
<style scoped lang="scss">
.container-main {
    overflow-y: hidden;
    overflow-x: hidden;
    padding: 10px;
}

.timeline-container {
    padding: 10px 5px;
    box-sizing: border-box;
    position: relative;
    margin-left: 20px;
}

/* 时间轴线 */
.timeline-container::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    width: 2px;
    background: #e0e0e0;
    margin-left: -10px;
}

.timeline-item {
    position: relative;
    padding-bottom: 20px;
}

/* 时间节点圆点 */
.timeline-item::before {
    content: '';
    position: absolute;
    top: 5px;
    left: -18px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #4a8bfc;
    border: 2px solid white;
    box-shadow: 0 0 0 2px #4a8bfc;
}

.receiver-name {
    font-size: 16px;
    font-weight: 800;
    margin-left: 10px;
}

.message-content {
    padding: 8px 0;
    font-size: 14px;
    color: #6f6d6d;
    margin-left: 10px;
    background: #f9f9f9;
    padding: 10px;
    margin-top: 10px;
    border-radius: 4px;
    // border-left: 3px solid #4a8bfc;
}

.create-time {
    padding: 5px 0;
    font-size: 12px;
    color: #999;
    margin-left: 10px;
    font-style: italic;
}

.new-item {
    display: flex;
    justify-content: flex-start;

    .item {
        padding: 5px;
        box-sizing: border-box;

        img {
            width: 168px;
            height: 104px;
            border-radius: 5px;
        }

    }

    .item-buttom {
        padding: 5px;
        box-sizing: border-box;

        .title {
            font-size: 16px;
            font-weight: 800;
        }

    }
}
</style>