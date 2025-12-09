<template>
    <div style="position: relative;">
        <img :src="activeData.cover" 
             style="min-height: 198px; max-height: 288px; object-fit: cover;"
             :style="{ width: width, borderRadius: borderRadius }" />
        <div class="tip-container">
            <div class="tip-name" @click="onClick">{{ activeData.name }}</div>
            <div class="point-container">
                <span v-for="(point, indexPoint) in data" 
                      :key="indexPoint"
                      @click.stop="switchTo(indexPoint)"
                      :class="{ 'active-point': indexPoint === index }"
                ></span>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: "Banner",
    props: {
        data: {
            type: Array,
            required: true
        },
        width: {
            type: String,
            default: '100%'
        },
        borderRadius: {
            type: String,
            default: '10px'
        },
        time: {
            type: Number,
            default: 3000
        }
    },
    watch: {
        data: {
            handler(newData) {
                if (newData.length) {
                    this.index = 0
                    this.activeData = { ...newData[0] }
                    this.config()
                }
            },
            deep: true,
            immediate: true
        }
    },
    data() {
        return {
            activeData: {},
            index: 0,
            timer: null
        }
    },
    methods: {
        onClick() {
            this.$emit('on-click', this.activeData)
        },
        config() {
            if (this.timer) clearInterval(this.timer)
            if (!this.data.length) return
            
            this.timer = setInterval(() => {
                this.index = (this.index + 1) % this.data.length
                this.activeData = { ...this.data[this.index] }
            }, this.time)
        },
        switchTo(indexPoint) {
            if (indexPoint >= 0 && indexPoint < this.data.length) {
                this.index = indexPoint
                this.activeData = { ...this.data[this.index] }
                this.config()
            }
        }
    },
    beforeDestroy() {
        if (this.timer) clearInterval(this.timer)
    }
}
</script>

<style scoped lang="scss">
.tip-container {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px 0;
    background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
    border-radius: 0 0 10px 10px;
}

.tip-name {
    color: white;
    font-size: 14px;
    text-align: center;
    margin-bottom: 8px;
    cursor: pointer;
    transition: color 0.3s;
    padding: 0 12px;

    &:hover {
        color: rgb(56, 183, 129);
    }
}

.point-container {
    display: flex;
    justify-content: center;
    gap: 8px;

    span {
        width: 14px;
        height: 10px;
        background: rgba(255, 255, 255, 0.5);
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s ease;

        &.active-point {
            width: 30px;
            background: rgb(223, 233, 118);
        }

        &:hover:not(.active-point) {
            background: rgba(255, 255, 255, 0.8);
        }
    }
}
</style>