-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: nexus_ai
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `blog`
--

DROP TABLE IF EXISTS `blog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `userId` bigint NOT NULL,
  `title` varchar(512) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '标题',
  `coverImg` varchar(1024) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '封面',
  `content` text COLLATE utf8mb3_bin NOT NULL COMMENT '内容',
  `thumbCount` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blog`
--

LOCK TABLES `blog` WRITE;
/*!40000 ALTER TABLE `blog` DISABLE KEYS */;
INSERT INTO `blog` VALUES (1,1,'Spring Boot 入门教程','https://example.com/img/springboot.png','这是一篇关于Spring Boot快速上手的博客文章，包含环境搭建、核心注解等内容。',1,'2026-04-19 17:32:15','2026-04-27 11:15:17'),(2,1,'MySQL 索引优化实战',NULL,'在开发中，索引是提升查询速度的关键。本文分享了B+树索引原理以及如何避免索引失效的常见场景。',0,'2026-04-19 17:32:15','2026-04-25 17:46:16'),(3,2,'前端 Vue3 组合式API','https://example.com/img/vue3.png','Vue3 的组合式 API 让代码逻辑更清晰，一起来看看怎么写 setup 和响应式数据。',0,'2026-04-19 17:32:15','2026-04-19 17:32:15'),(4,3,'Java 泛型核心原理','https://example.com/img/java.png','泛型是Java的重要特性，讲解泛型的类型擦除、通配符使用以及实际开发场景。',0,'2026-04-19 17:32:15','2026-04-25 18:08:05'),(5,3,'Redis 缓存实战',NULL,'Redis 高性能缓存中间件，本文讲解字符串、哈希、列表等常用数据结构及实战应用。',0,'2026-04-19 17:32:15','2026-04-19 17:32:15'),(6,4,'Spring Cloud 微服务','https://example.com/img/cloud.png','微服务架构入门，讲解服务注册、配置中心、网关等核心组件的使用。',0,'2026-04-20 20:02:15','2026-04-21 21:17:40'),(7,1,'接口测试',NULL,'这是进行创建博客的接口测试',0,'2026-04-27 15:27:38','2026-04-27 15:27:38');
/*!40000 ALTER TABLE `blog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论主键ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `blog_id` bigint NOT NULL COMMENT '所属博客/文章ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID（0=顶级评论）',
  `content` text COLLATE utf8mb4_bin NOT NULL COMMENT '评论内容',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态：0-待审核 1-通过 2-驳回 3-人工复核',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_blog_id` (`blog_id`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_blog_audit_time` (`blog_id`,`audit_status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='评论表（含AI审核状态，不可更新）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (1,1,1,0,'这篇文章写的不错啊',0,'2026-04-29 14:58:33'),(2,1,2,0,'正在测试ai审核',0,'2026-04-29 15:00:11'),(3,1,3,0,'333正在测试ai审核',0,'2026-04-29 15:03:06'),(4,1,4,0,'这篇文章写的可以喔',3,'2026-04-29 15:21:50'),(5,1,5,0,'这篇文章写的可以喔 我正在被ai审核',1,'2026-04-29 16:08:55'),(6,1,6,0,'一个非常完善的ai审核模块已经完成啦',1,'2026-04-29 18:51:46'),(7,1,7,0,'写得真一般，我感觉作者好色，有点弱智垃圾',2,'2026-04-29 18:52:36'),(8,1,6,0,'写得真一般，我感觉作者好色，有点弱智垃圾',2,'2026-04-29 18:52:57');
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `pid` bigint NOT NULL AUTO_INCREMENT,
  `pname` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=30002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'文章基础点赞','article:like'),(2,'文章评论','article:comment'),(3,'文章浏览','article:view'),(4,'文章收藏','article:collect'),(5,'查看审核通过的内容','content:audit:passed'),(6,'内容创作','article:create'),(7,'编辑文章','article:edit'),(8,'删除文章','article:delete'),(9,'互动管理','interaction:manage'),(10,'查看自身内容审核进度','audit:progress:view'),(11,'批量创造','article:batch:create'),(12,'员工管理','staff:manage'),(13,'商业变现','merchant:monetize'),(14,'查看企业内容审核统计','enterprise:audit:stats'),(15,'管控用户','user:control'),(16,'管控内容','content:control'),(17,'系统配置','system:config'),(18,'分配审核员权限','auditor:assign'),(19,'内容审核','content:audit:internal'),(20,'审核日志查询权限','audit:log:query'),(21,'查看AI审核辅助结果','audit:ai:assist');
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `rid` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `rname` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '角色名称',
  `rcode` varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '角色编码',
  PRIMARY KEY (`rid`)
) ENGINE=InnoDB AUTO_INCREMENT=30002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'普通用户','USER'),(2,'创作者','CREATOR'),(3,'企业用户','ENTERPRISE'),(4,'管理员','ADMIN'),(5,'内容审核员','AUDITOR');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `rid` bigint DEFAULT NULL,
  `pid` bigint DEFAULT NULL,
  KEY `idx_rid` (`rid`),
  KEY `idx_pid` (`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (2,6),(2,7),(2,8),(2,9),(2,10),(3,11),(3,12),(3,13),(3,14),(4,1),(4,2),(4,3),(4,4),(4,5),(4,6),(4,7),(4,8),(4,9),(4,10),(4,11),(4,12),(4,13),(4,14),(4,15),(4,16),(4,17),(4,18),(5,19),(5,20),(5,21),(1,1),(1,2),(1,3),(1,4),(1,5);
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `thumb`
--

DROP TABLE IF EXISTS `thumb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `thumb` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `userId` bigint NOT NULL,
  `blogId` bigint NOT NULL,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_userId_blogId` (`userId`,`blogId`)
) ENGINE=InnoDB AUTO_INCREMENT=30001 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `thumb`
--

LOCK TABLES `thumb` WRITE;
/*!40000 ALTER TABLE `thumb` DISABLE KEYS */;
INSERT INTO `thumb` VALUES (1,1,1,'2026-04-27 11:15:15');
/*!40000 ALTER TABLE `thumb` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `username` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `phone` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `email` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `password` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '密码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30001 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'张三','12787876660','','$2a$10$RI/oJ28iaWsbG6QPkqUihOO9xvCNLylKLfZiezonq/k7MmyNKodj.');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `uid` bigint DEFAULT NULL,
  `rid` bigint DEFAULT NULL,
  KEY `idx_uid` (`uid`),
  KEY `idx_rid` (`rid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES (1,1);
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-29 20:50:58
