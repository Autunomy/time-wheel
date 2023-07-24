create database deduplication;
use deduplication;
drop table if exists timer_wheel_task;

create table timer_wheel_task(
     uid varchar(32) comment '唯一标识',
     expire_time long comment '过期时间戳',
     task text comment '转发mq的信息'
) comment '时间轮的任务表';