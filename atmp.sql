/*
Navicat MySQL Data Transfer

Source Server         : atmp
Source Server Version : 50621
Source Host           : 192.168.56.15:3306
Source Database       : atmp

Target Server Type    : MYSQL
Target Server Version : 50621
File Encoding         : 65001

Date: 2017-11-21 17:56:55
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for `sys_modules`
-- ----------------------------
DROP TABLE IF EXISTS `sys_modules`;
CREATE TABLE `sys_modules` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `proj_id` int(3) DEFAULT NULL,
  `project` varchar(20) DEFAULT NULL,
  `module` varchar(80) DEFAULT NULL,
  `tag` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_projects`
-- ----------------------------
DROP TABLE IF EXISTS `sys_projects`;
CREATE TABLE `sys_projects` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `project` varchar(80) DEFAULT NULL,
  `tag` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_taskquene`
-- ----------------------------
DROP TABLE IF EXISTS `sys_taskquene`;
CREATE TABLE `sys_taskquene` (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `taskid` varchar(80) DEFAULT NULL,
  `tset` varchar(100) DEFAULT NULL,
  `proj` varchar(80) DEFAULT '',
  `projtag` varchar(20) DEFAULT '',
  `creattime` datetime DEFAULT NULL,
  `creater` varchar(40) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `starttime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_testreports`
-- ----------------------------
DROP TABLE IF EXISTS `sys_testreports`;
CREATE TABLE `sys_testreports` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `trname` varchar(80) DEFAULT NULL,
  `project` varchar(80) DEFAULT NULL,
  `testset` varchar(80) DEFAULT NULL,
  `owner` varchar(20) DEFAULT NULL,
  `result` varchar(20) DEFAULT NULL,
  `starttime` datetime DEFAULT '0001-01-01 00:00:00',
  `creattime` datetime DEFAULT '0001-01-01 00:00:00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_testscripts`
-- ----------------------------
DROP TABLE IF EXISTS `sys_testscripts`;
CREATE TABLE `sys_testscripts` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `tsid` varchar(25) DEFAULT NULL,
  `name` varchar(30) DEFAULT NULL,
  `project` varchar(20) DEFAULT NULL,
  `module` varchar(20) DEFAULT NULL,
  `owner` varchar(20) DEFAULT NULL,
  `upload_time` datetime DEFAULT NULL,
  `lok` int(3) DEFAULT '0',
  `tcf` varchar(8) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_testsets`
-- ----------------------------
DROP TABLE IF EXISTS `sys_testsets`;
CREATE TABLE `sys_testsets` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) DEFAULT NULL,
  `creattime` datetime DEFAULT NULL,
  `creater` varchar(20) DEFAULT NULL,
  `project` varchar(20) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_usr_purview`
-- ----------------------------
DROP TABLE IF EXISTS `sys_usr_purview`;
CREATE TABLE `sys_usr_purview` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `usrname` varchar(100) NOT NULL DEFAULT '',
  `Task_add` varchar(80) NOT NULL DEFAULT 'y',
  `Task_stop` varchar(80) NOT NULL DEFAULT 'y',
  `TR_review` varchar(80) NOT NULL DEFAULT 'y',
  `TR_del` varchar(80) NOT NULL DEFAULT 'y',
  `Tset_add` varchar(80) NOT NULL DEFAULT 'y',
  `Tset_del` varchar(80) NOT NULL DEFAULT 'y',
  `Tset_update` varchar(80) NOT NULL DEFAULT 'y',
  `TS_upload` varchar(80) NOT NULL DEFAULT 'y',
  `TS_del` varchar(80) NOT NULL DEFAULT 'y',
  `Ldap_conf` varchar(80) NOT NULL DEFAULT 'y',
  `Ldap_sync` varchar(80) NOT NULL DEFAULT 'y',
  `User_add` varchar(80) NOT NULL DEFAULT 'y',
  `User_del` varchar(80) NOT NULL DEFAULT 'y',
  `Purview_conf` varchar(80) NOT NULL DEFAULT 'y',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `sys_usrdb`
-- ----------------------------
DROP TABLE IF EXISTS `sys_usrdb`;
CREATE TABLE `sys_usrdb` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `usrname` varchar(40) DEFAULT NULL,
  `passwd` varchar(40) DEFAULT NULL,
  `fullname` varchar(40) DEFAULT NULL,
  `dept` varchar(60) DEFAULT '',
  `role` varchar(30) DEFAULT NULL,
  `email` varchar(40) DEFAULT '',
  `mobile` varchar(20) DEFAULT '',
  `type` varchar(20) DEFAULT 'ldap',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of sys_usrdb
-- ----------------------------
INSERT INTO `sys_usrdb` VALUES ('1', 'admin', 'dcgejo', '系统管理员', '', '1', '', '', 'local');

-- ----------------------------
-- Table structure for `temp_task`
-- ----------------------------
DROP TABLE IF EXISTS `temp_task`;
CREATE TABLE `temp_task` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `name` varchar(40) DEFAULT NULL,
  `path` varchar(40) DEFAULT NULL,
  `policy` varchar(4) DEFAULT NULL,
  `status` varchar(12) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `tr_temp`
-- ----------------------------
DROP TABLE IF EXISTS `tr_temp`;
CREATE TABLE `tr_temp` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `tsname` varchar(100) DEFAULT NULL,
  `testname` varchar(100) DEFAULT NULL,
  `expectvalue` varchar(200) DEFAULT NULL,
  `factvalue` varchar(500) DEFAULT NULL,
  `testresult` varchar(20) DEFAULT NULL,
  `starttime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;