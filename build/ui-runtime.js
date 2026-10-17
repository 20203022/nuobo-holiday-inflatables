import { createApp, h } from 'vue';
import ElBacktop from 'element-plus/es/components/backtop/index.mjs';
import ElMessage from 'element-plus/es/components/message/index.mjs';
import ElNotification from 'element-plus/es/components/notification/index.mjs';
import 'element-plus/es/components/backtop/style/css';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/notification/style/css';

window.Vue = {createApp, h};
window.ElementPlus = {
    ElBacktop,
    install(app) { app.component('el-backtop', ElBacktop); },
    ElMessage,
    ElNotification
};
