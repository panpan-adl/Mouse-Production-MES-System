<script setup lang="ts">
import {computed} from 'vue'
const props=defineProps<{rows:{id:number;material_name:string;unit:string;safety_stock:number;current_quantity:number}[]}>()
const scale=(row:typeof props.rows[number])=>Math.max(1,Number(row.current_quantity),Number(row.safety_stock))*1.15
const shortCount=computed(()=>props.rows.filter(row=>Number(row.current_quantity)<Number(row.safety_stock)).length)
</script>
<template>
<section class="stock-safety panel">
<div class="stock-heading"><div><h2>库存安全监测</h2><p>当前库存 · {{shortCount}} 项低于安全线</p></div><router-link to="/inventory/trace">查看明细 ↗</router-link></div>
<div class="stock-legend"><span><i></i>现存量</span><span><i class="threshold"></i>安全库存</span></div>
<div class="stock-rows"><div v-for="row in rows" :key="row.id" class="stock-row"><div class="stock-label"><span :title="row.material_name">{{row.material_name}}</span><strong :class="{low:Number(row.current_quantity)<Number(row.safety_stock)}">{{Number(row.current_quantity).toLocaleString()}}<small> / {{Number(row.safety_stock).toLocaleString()}} {{row.unit}}</small></strong></div><div class="stock-track"><i :style="{width:`${Number(row.current_quantity)/scale(row)*100}%`}"></i><b :style="{left:`${Number(row.safety_stock)/scale(row)*100}%`}"></b></div></div><p v-if="!rows.length" class="stock-empty">暂无库存物料记录</p></div>
</section>
</template>
<style scoped>
.stock-safety{padding:20px;border:1px solid #e9e0d3;border-radius:14px;background:#fffdf9;min-width:0}.stock-heading{display:flex;justify-content:space-between;gap:10px;align-items:start}.stock-heading h2{font-size:13px;font-weight:600;color:#80684a}.stock-heading p{font-size:10px;color:#b09b7f;margin-top:7px}.stock-heading a{font-size:10px;color:#a47f4f;white-space:nowrap}.stock-legend{display:flex;gap:15px;margin:14px 0;font-size:9px;color:#b09c80}.stock-legend span{display:flex;align-items:center;gap:5px}.stock-legend i{width:10px;height:4px;background:#ae9b76;border-radius:2px}.stock-legend .threshold{width:2px;height:8px;background:#c48d61}.stock-rows{max-height:240px;overflow:auto;padding-right:7px}.stock-row{margin-bottom:17px}.stock-label{display:flex;justify-content:space-between;gap:10px;font-size:10px;align-items:baseline}.stock-label>span{color:#8d775b;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.stock-label strong{font-weight:500;white-space:nowrap;color:#8a8d6a}.stock-label strong.low{color:#bb8669}.stock-label small{font-size:9px;color:#b7a68e;font-weight:400}.stock-track{height:5px;background:#f0e9dd;border-radius:4px;margin-top:8px;position:relative}.stock-track>i{display:block;height:100%;background:#ae9b76;border-radius:4px}.stock-track>b{position:absolute;top:-2px;height:9px;border-left:2px solid #c48d61}.stock-empty{padding:35px 10px;text-align:center;font-size:12px;color:#b09b7f}
</style>
