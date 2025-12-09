import Vue from "vue";
import VueRouter from "vue-router";
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import { getToken } from "@/utils/storage.js";
import echarts from 'echarts';
Vue.prototype.$echarts = echarts;
Vue.use(ElementUI);
Vue.use(VueRouter);

const routes = [
  {
    path: "*",
    redirect: "/login"
  },
  {
    path: "/login",
    component: () => import(`@/views/login/Login.vue`)
  },
  {
    path: "/register",
    component: () => import(`@/views/register/Register.vue`)
  },
  {
    path: "/message",
    component: () => import(`@/views/user/Message.vue`)
  },
  {
    path: "/record",
    component: () => import(`@/views/user/Record.vue`)
  },
  {
    path: "/admin",
    component: () => import(`@/views/admin/Home.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        path: "/adminLayout",
        name: '仪表盘',
        icon: 'el-icon-pie-chart',
        component: () => import(`@/views/admin/Main.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/userManage",
        name: '用户管理',
        icon: 'el-icon-user',
        component: () => import(`@/views/admin/UserManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/tagsManage",
        name: '资讯分类',
        icon: 'el-icon-house',
        component: () => import(`@/views/admin/TagsManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/newsManage",
        name: '资讯管理',
        icon: 'el-icon-document',
        component: () => import(`@/views/admin/NewsManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/healthModelConfigManage",
        name: '模型管理',
        icon: 'el-icon-files',
        component: () => import(`@/views/admin/HealthModelConfigManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/userHealthManage",
        name: '健康记录',
        icon: 'el-icon-c-scale-to-original',
        component: () => import(`@/views/admin/UserHealthManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/messageManage",
        name: '消息管理',
        icon: 'el-icon-message',
        component: () => import(`@/views/admin/MessageManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/evaluationsManage",
        name: '评论管理',
        icon: 'el-icon-chat-dot-round',
        component: () => import(`@/views/admin/EvaluationsManage.vue`),
        meta: { requireAuth: true },
      },
    ]
  },
  {
    path: "/user",
    component: () => import(`@/views/user/Main.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        name: '资讯',
        path: "/news-record",
        component: () => import(`@/views/user/Home.vue`),
        meta: {
          requireAuth: true,
        },
      },
      {
        name: '我的收藏夹',
        path: "/my-save",
        icon: 'el-icon-document-copy',
        component: () => import(`@/views/user/NewsSave.vue`),
        meta: {
          requireAuth: true,
        },
      },
      {
        name: '健康可视化',
        path: "/user-health-model",
        icon: 'el-icon-data-analysis',
        component: () => import(`@/views/user/UserHealthModel.vue`),
        meta: {
          requireAuth: true,
        },
      },
      {
        name: '健康资讯详情',
        path: "/news-detail",
        component: () => import(`@/views/user/NewsDetail.vue`),
        meta: {
          requireAuth: true,
        },
        isHidden: true,
      },
      {
        name: '搜索页',
        path: "/search-detail",
        component: () => import(`@/views/user/Search.vue`),
        meta: {
          requireAuth: true,
        },
        isHidden: true,
      },
    ]
  }
];
const router = new VueRouter({
  routes,
  mode: 'history'
});
router.beforeEach((to, from, next) => {
  // 放行登录页和注册页
  if (to.path === '/login' || to.path === '/register') {
    return next();
  }

  // 检查需要认证的路由
  if (to.matched.some(record => record.meta.requireAuth)) {
    const token = getToken();

    // 未登录情况处理
    if (!token) {
      return next({
        path: '/login',
        query: { redirect: to.fullPath } // 记录目标路由
      });
    }

    // 已登录时的权限检查
    try {
      const role = parseInt(sessionStorage.getItem('role'));
      
      // 管理员路径检查
      if (to.matched[0].path === '/admin' && role !== 1) {
        clearToken();
        return next("/login"); //返回登录页
      }

      // 用户路径检查
      if (to.matched[0].path === '/user' && role !== 2) {
        clearToken();
        return next("/login"); //返回登录页
      }

      return next();
    } catch (error) {
      console.error('权限检查失败:', error);
      return next('/login');
    }
  }

  // 普通页面直接放行
  next();
});
export default router;
