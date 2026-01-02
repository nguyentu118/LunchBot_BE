-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: lunchbot_db
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `addresses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `building` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `district` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `district_id` int DEFAULT NULL,
  `is_default` bit(1) NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province_id` int DEFAULT NULL,
  `street` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ward` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ward_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1fa36y2oqhao3wgg2rw1pi459` (`user_id`),
  CONSTRAINT `FK1fa36y2oqhao3wgg2rw1pi459` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `addresses`
--

LOCK TABLES `addresses` WRITE;
/*!40000 ALTER TABLE `addresses` DISABLE KEYS */;
INSERT INTO `addresses` VALUES (1,'','Nguyễn Anh Tú','2025-12-23 14:14:34.609899','Huyện Lương Sơn',1968,_binary '\0','09863413562','Hòa Bình',267,'số 88','Xã Thành Lập','230716',2,'2025-12-23 14:21:53.071572'),(2,'','Nguyễn Văn A','2025-12-23 14:15:04.707401','Huyện Đông Hưng',1715,_binary '','0986341265','Thái Bình',226,'số 8','Xã Đông Động','260409',2,NULL),(3,'','Nguyễn Anh Tú','2025-12-23 14:26:45.516029','Huyện Nậm Nhùn',1984,_binary '\0','0986341362','Lai Châu',264,'số 62','Xã Nậm Manh','70808',2,'2025-12-23 14:27:21.196358'),(4,'','Nguyen Anh Tu','2025-12-24 08:53:01.083454','Huyện Lục Yên',1967,_binary '','0986482635','Yên Bái',263,'so 8','Xã Tô Mậu','130920',3,NULL),(5,'','Nguyen Van A','2025-12-24 09:06:43.819440','Huyện Cao Phong',2087,_binary '','0985648914','Hòa Bình',267,'so 25','Xã Thung Nai','231110',12,NULL),(6,'','Nguyen Van B','2025-12-24 09:26:32.984013','Huyện Lạc Sơn',2156,_binary '','0985214631','Hòa Bình',267,'so 8','Xã Yên Nghiệp','230528',4,NULL),(7,'','Nguyễn Anh Tú','2025-12-24 10:20:31.308420','Thị xã Đông Hòa',3184,_binary '\0','0985214624','Phú Yên',260,'số 45','Xã Hòa Xuân Nam','390709',2,NULL);
/*!40000 ALTER TABLE `addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) DEFAULT NULL,
  `quantity` int NOT NULL,
  `cart_id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `price` decimal(38,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  KEY `FKqf96jt4hthdxw36s3ebnq1yns` (`dish_id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`),
  CONSTRAINT `FKqf96jt4hthdxw36s3ebnq1yns` FOREIGN KEY (`dish_id`) REFERENCES `dishes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=208 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (57,'2025-12-17 10:41:32.048421',2,9,34,66000.00),(61,'2025-12-17 11:11:21.351459',3,9,6,70000.00),(81,'2025-12-22 08:35:56.685124',1,6,33,89000.00),(200,'2025-12-31 08:43:30.649729',1,13,13,149000.00),(207,'2025-12-31 13:45:07.743578',1,5,33,89000.00);
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK64t7ox312pqal3p7fg9o503c2` (`user_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `carts`
--

LOCK TABLES `carts` WRITE;
/*!40000 ALTER TABLE `carts` DISABLE KEYS */;
INSERT INTO `carts` VALUES (5,'2025-12-31 13:45:07.744130',2),(6,'2025-12-24 09:26:07.785875',4),(7,'2025-12-16 13:45:41.292529',11),(8,'2025-12-16 16:01:10.590532',5),(9,'2025-12-17 11:11:38.745349',6),(10,'2025-12-24 16:12:17.631369',3),(11,'2025-12-24 09:06:06.840980',12),(12,'2025-12-31 09:12:20.525363',10),(13,'2025-12-31 08:43:30.665351',1);
/*!40000 ALTER TABLE `carts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `icon_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKoul14ho7bctbefv8jywp5v3i2` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,NULL,'Ăn sáng','an-sang'),(2,NULL,'Cơm','com'),(3,NULL,'Đồ uống','do-uong'),(4,NULL,'Đồ ăn vặt','do-an-vat'),(5,NULL,'Hải sản','hai-san'),(6,NULL,'Đồ chay','do-chay'),(7,NULL,'Gà rán','ga-ran'),(8,NULL,'KFC','KFC'),(9,NULL,'Đồ ăn  nhanh','do-an-nhanh'),(13,NULL,'Đồ nhậu','do-nhau'),(14,NULL,'Món chính','mon-chinh');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `discount_type` enum('FIXED_AMOUNT','PERCENTAGE') NOT NULL,
  `discount_value` decimal(10,0) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `min_order_value` decimal(38,2) NOT NULL DEFAULT '0.00',
  `usage_limit` int NOT NULL,
  `used_count` int NOT NULL,
  `valid_from` date NOT NULL,
  `valid_to` date NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKpoarrhsu1hc36cwoed2o68hbl` (`merchant_id`),
  CONSTRAINT `FKpoarrhsu1hc36cwoed2o68hbl` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupons`
--

LOCK TABLES `coupons` WRITE;
/*!40000 ALTER TABLE `coupons` DISABLE KEYS */;
INSERT INTO `coupons` VALUES (1,'BLACKFRIDAY','2025-12-16 14:53:05.682358','FIXED_AMOUNT',10000,_binary '\0',100000.00,25,2,'2024-01-01','2026-01-31',2),(2,'FREESHIP','2025-12-16 14:54:09.361991','FIXED_AMOUNT',20000,_binary '',0.00,10,5,'2025-12-16','2026-01-31',2),(3,'FREESHIP','2025-12-16 14:57:15.406986','FIXED_AMOUNT',10000,_binary '',150000.00,20,1,'2025-12-16','2026-01-31',4),(5,'FREESHIP','2025-12-16 15:49:44.546029','FIXED_AMOUNT',150000,_binary '',150000.00,10,0,'2025-12-16','2026-01-31',3),(6,'BLACKFRIDAY','2025-12-16 15:56:57.522490','PERCENTAGE',5,_binary '\0',250000.00,15,0,'2025-12-16','2026-01-31',3),(8,'NOEL','2025-12-18 10:09:47.576902','FIXED_AMOUNT',10000,_binary '',100000.00,20,10,'2024-01-01','2026-01-31',2),(10,'12-12','2025-12-18 14:18:24.390598','FIXED_AMOUNT',25000,_binary '',0.00,100,7,'2025-12-18','2026-01-31',2),(11,'SINH NHẬT','2025-12-18 14:18:46.196664','PERCENTAGE',15,_binary '',500000.00,50,6,'2025-12-18','2026-01-31',2),(12,'FREESHIP','2025-12-24 08:41:01.385666','FIXED_AMOUNT',25000,_binary '',0.00,100,4,'2025-12-24','2026-01-31',1),(13,'BLACKFRIDAY','2025-12-24 08:41:15.364973','PERCENTAGE',5,_binary '',0.00,100,2,'2025-12-24','2026-01-31',1),(14,'NOEL','2025-12-24 08:41:42.062903','FIXED_AMOUNT',50000,_binary '\0',250000.00,100,2,'2025-12-10','2026-01-31',1),(18,'12-12','2025-12-25 14:41:19.677356','FIXED_AMOUNT',30000,_binary '',0.00,100,0,'2025-12-25','2026-01-31',1),(19,'NOEL','2025-12-25 14:42:06.576909','PERCENTAGE',10,_binary '',500000.00,100,1,'2025-12-25','2026-01-31',8),(20,'FREESHIP','2025-12-25 15:35:45.325987','FIXED_AMOUNT',20000,_binary '',0.00,100,0,'2025-12-25','2026-01-31',8),(21,'BLACKFRIDAY','2025-12-26 21:21:26.887328','FIXED_AMOUNT',15000,_binary '',0.00,100,0,'2025-12-26','2026-01-31',8);
/*!40000 ALTER TABLE `coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_category`
--

DROP TABLE IF EXISTS `dish_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_category` (
  `dish_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`dish_id`,`category_id`),
  KEY `FKpj7obdht6kakpqrwu7yrrdqef` (`category_id`),
  CONSTRAINT `FKo2g23cekq3d0xyc8jrpmdvvpp` FOREIGN KEY (`dish_id`) REFERENCES `dishes` (`id`),
  CONSTRAINT `FKpj7obdht6kakpqrwu7yrrdqef` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_category`
--

LOCK TABLES `dish_category` WRITE;
/*!40000 ALTER TABLE `dish_category` DISABLE KEYS */;
INSERT INTO `dish_category` VALUES (1,1),(2,1),(3,1),(27,1),(28,1),(29,1),(30,1),(53,1),(54,1),(4,2),(22,2),(23,2),(24,2),(53,2),(54,2),(55,2),(10,3),(11,3),(16,3),(20,3),(21,3),(25,3),(29,3),(35,3),(7,4),(8,4),(9,4),(10,4),(11,4),(19,4),(33,4),(36,4),(46,4),(12,5),(13,5),(14,5),(15,5),(24,5),(38,5),(39,5),(4,6),(5,6),(6,6),(7,6),(40,6),(18,7),(19,7),(23,7),(18,8),(19,8),(20,8),(21,8),(6,9),(8,9),(9,9),(18,9),(19,9),(28,9),(30,9),(33,9),(34,9),(46,9),(31,13),(32,13),(33,13),(34,13),(35,13),(36,13),(37,13),(18,14),(31,14),(32,14),(37,14),(38,14),(39,14);
/*!40000 ALTER TABLE `dish_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dish_images`
--

DROP TABLE IF EXISTS `dish_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `display_order` int NOT NULL,
  `image_url` varchar(255) NOT NULL,
  `is_primary` bit(1) DEFAULT NULL,
  `public_id` varchar(255) DEFAULT NULL,
  `dish_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKq00qxymg9sbmxn75leo2vdjuy` (`dish_id`),
  CONSTRAINT `FKq00qxymg9sbmxn75leo2vdjuy` FOREIGN KEY (`dish_id`) REFERENCES `dishes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dish_images`
--

LOCK TABLES `dish_images` WRITE;
/*!40000 ALTER TABLE `dish_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `dish_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dishes`
--

DROP TABLE IF EXISTS `dishes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dishes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `discount_price` decimal(10,2) DEFAULT NULL,
  `images_urls` json DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `is_recommended` bit(1) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_count` int NOT NULL DEFAULT '0',
  `preparation_time` int DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `service_fee` decimal(10,2) DEFAULT '0.00',
  `updated_at` datetime(6) DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1d31tnw89e0gqkh9eobve8kv3` (`merchant_id`),
  CONSTRAINT `FK1d31tnw89e0gqkh9eobve8kv3` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dishes`
--

LOCK TABLES `dishes` WRITE;
/*!40000 ALTER TABLE `dishes` DISABLE KEYS */;
INSERT INTO `dishes` VALUES (1,'2025-12-14 10:04:22.241393','Bún Chả Hà Nội: Hương vị đậm đà của Thủ Đô',45000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681455/ftgxqyo5v3bhmjzktlln.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681457/tv8tntw3pzfoikuivrlo.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681459/kj0ibfkkpzjsny8zwn3l.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681460/l0wb0hovkbdfvi8ysllh.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681461/s9frnbeje0ae5sxy0h9z.jpg\"]',_binary '',_binary '','Bún chả Hà Nội',1,NULL,50000.00,0.00,'2025-12-23 10:49:03.297939',16,6),(2,'2025-12-14 10:08:55.114443','Phở bò tái được chần chín tái ',50000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681730/tcvieilrbv7lwrn4eern.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681732/thkacz91mouqbxddypqc.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681733/rcgwso4gg7xi7vui2q09.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681734/upju27w4mcxr3ispnw5s.jpg\"]',_binary '',_binary '','Phở bò tái',1,0,55000.00,0.00,'2025-12-30 10:41:03.318053',4,6),(3,'2025-12-14 10:11:06.881260','Phở gà Hà Nội: Hương vị truyền thống thủ đô',50000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681860/xtvyszogdtictneswxnu.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681862/flr3onx9fnnyqgdngjiy.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681863/db8crhoox8pgchxj7eba.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681864/uoz1p1s9fzddasfviivz.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765681866/mr0wkfhboyn05az5xbee.jpg\"]',_binary '',_binary '','Phở Gà',2,NULL,50000.00,0.00,'2025-12-24 10:54:54.903469',222,6),(4,'2025-12-14 10:17:54.222830','',30000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682270/ld0xmunec40y5oiosw3s.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682272/nlc8po2gt0fuwrgoqzic.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682273/ehpmtzgjdhcppasglbt1.png\"]',_binary '',_binary '','Cơm chay thập cẩm',1,NULL,30000.00,0.00,'2025-12-16 08:33:34.479035',20,5),(5,'2025-12-14 10:20:26.940317','',55000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682422/siadsidw3zhwjjlxk2p5.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682423/s0kschmc5lrayqijq0qz.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682425/m9jutrgodmxv7w4cw8ms.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682426/d5n88wekpw4w5by0zavx.jpg\"]',_binary '',_binary '','Đậu hũ kho nấm',2,0,60000.00,0.00,'2025-12-30 09:35:18.574030',6,5),(6,'2025-12-14 10:22:56.438668','',60000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682570/p5h2gvrskwv2y3xrlomy.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682571/xmq3b2givwvxxdamyjee.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682573/qcvhfy3caqyg58tkp0wi.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682574/agfgf2inoe3vdbfycvur.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682575/rya52q3ub9cpmsd8wcrw.jpg\"]',_binary '',_binary '','Gỏi cuốn chay',6,NULL,70000.00,0.00,'2025-12-30 10:28:19.273741',116,5),(7,'2025-12-14 10:24:48.659532','',20000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682682/in4yiobvwdfuurc0ltes.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682684/dw97xtvjl3jyo6lxaebp.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682685/mzhuusrnuwkxhxrxiycq.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682686/rvauco3yaaj6rs34jkzw.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682687/rcmmkrkugrfvgn4jrigz.jpg\"]',_binary '',_binary '','Chè đậu xanh',1,NULL,25000.00,0.00,'2025-12-31 14:08:07.024508',73,5),(8,'2025-12-14 10:27:22.100014','Siêu to khổng lồ',20000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682832/yxjzuqcziuzv581tgkbx.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682834/z5wv7bl87ery5uc6nttg.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682836/gvc49jovjblcj6nig2sn.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682838/xvf8rapw1phxotajbyr5.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682841/ly1gb7takudg14typnes.png\"]',_binary '',_binary '','Bánh tráng trộn siêu cay',1,15,20000.00,0.00,'2025-12-31 09:12:14.743367',20,4),(9,'2025-12-14 10:28:46.489306','',40000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682919/th8z9yxypetaymir3u4a.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682921/si3xtic8lxw2slwxjxhs.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682923/dd7ueivlqb5helzqmm2y.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682924/bhia0ps7asp2glwtk4gb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682925/rednjksrvswzi0cshh5o.jpg\"]',_binary '',_binary '','Cá viên chiên',4,NULL,40000.00,0.00,'2025-12-31 09:12:17.819175',25,4),(10,'2025-12-14 10:30:18.957164','',35000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683011/vjamvlsbax3juuthb07j.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683012/figjv2zxribjvlwtfahy.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683014/v33s5ccwhydpiq6fs1ef.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683016/qcjod5m45uzu4x0vvr4e.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683018/n9e3kaaxvythtjc5epja.jpg\"]',_binary '',_binary '','Sữa tươi trân châu đường đen',1,NULL,35000.00,0.00,'2025-12-31 09:12:19.259349',14,4),(11,'2025-12-14 10:31:50.722656','',55000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683104/t4vadgpmqahihsf66acu.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683106/xbqw6iyg3c0mpqc9jog2.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683107/qfhid2dlaestyllpjhgf.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683108/m5fvuh95yudp7vfrqda3.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683110/udanxcspjkmksag2edzo.jpg\"]',_binary '',_binary '','Matcha Latte',2,NULL,59000.00,0.00,'2025-12-30 10:38:30.817704',18,4),(12,'2025-12-14 10:35:18.258477','',99000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683313/twua1gbeyd54yb8ph4uo.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683315/dposs5b500jco3ksezm7.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683316/s03duzcujg4watnrnu8l.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683317/alxs5vrtusabtvmu2tfb.jpg\"]',_binary '',_binary '','Tôm hấp sả',3,NULL,110000.00,0.00,'2025-12-23 20:15:07.817020',19,3),(13,'2025-12-14 10:37:26.858920','',139000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683440/r3vjld6msddh0rlawww0.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683441/qwxzuqbcpyepeypjil0s.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683443/xaoca5zptitqb2ub20rb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683445/d855uc2grgkd5em6wgsl.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683446/igj5y9cj1nxtfbz6n7lz.webp\"]',_binary '',_binary '','Mực nướng muối ớt',1,NULL,149000.00,0.00,'2025-12-31 08:43:38.470286',327,3),(14,'2025-12-14 10:39:30.829208','',189000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683565/atry83nasnyn56txa1b1.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683567/ovdjkdkbe84ud5vttxcs.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683568/twadaga76ahrmglqgmaw.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683570/sedpfv54kr5omlnbuqev.jpg\"]',_binary '',_binary '','Nghêu hấp thái',1,NULL,209000.00,0.00,'2025-12-30 10:38:26.089896',4,3),(15,'2025-12-14 10:41:01.055759','',129000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683655/lft97zuyuf2pk7s1caz5.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683656/w5cn7qudzqdvszmrivn1.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683658/vnrhdaz01kukdzaqugrx.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683659/d1z7tuuzrhoyflvyvgil.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683660/n5swnb9wfey2cfgp6hsg.jpg\"]',_binary '',_binary '','Cá nướng giấy bạc',4,NULL,149000.00,0.00,'2025-12-23 20:13:27.872661',40,3),(16,'2025-12-14 10:41:51.376017','',20000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683710/wwuxu0jgmqawazwuazm1.jpg\"]',_binary '',_binary '','Bia Tiger',1,NULL,20000.00,0.00,'2025-12-14 10:58:47.313319',456,3),(18,'2025-12-14 10:46:21.054123','',57000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683977/e2oqvgrwr1hljhmzh6eb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683979/p0e44rk48azdpvfxz7jk.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683980/f70cgatsocydz5vdrvyo.jpg\"]',_binary '',_binary '','Burger gà',2,0,59000.00,0.00,'2025-12-30 10:38:53.902625',26,2),(19,'2025-12-14 10:48:20.463627','',45000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684094/ipjlmfj0vsjejdyfdctw.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684095/tjszwgaqrmhbzok28hqf.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684096/grs7jwhrecsza4lfzkno.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684098/bhmupvpvlvx0ooqsqzgm.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684099/cylstq3jtjnoitmfgjzm.jpg\"]',_binary '',_binary '','Khoai tây chiên',3,0,49000.00,0.00,'2025-12-30 10:38:20.576271',10,2),(20,'2025-12-14 10:50:05.649821','',15000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684204/cizrbtbbpbk2hyp6ikij.jpg\"]',_binary '',_binary '','Pepsi',1,0,15000.00,0.00,'2025-12-30 10:43:27.401375',667,2),(21,'2025-12-14 10:50:34.573236','',15000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684234/ynr2bt9p0f6lsm98uhzn.png\"]',_binary '',_binary '','Coca Cola',3,NULL,15000.00,0.00,'2025-12-16 09:48:23.598775',8,2),(22,'2025-12-14 10:53:25.057646','',45000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684399/zirom3zwy9obuplmbtkk.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684400/gpfgonypkzwrbztt2mp7.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684402/glzzshfx4myhzdfi9az4.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684403/hn1oxtpraga0anevbcnp.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684404/asy48y4be1pavtuji4kj.jpg\"]',_binary '',_binary '','Cơm tấm sườn nướng',8,NULL,49000.00,0.00,'2025-12-30 10:42:46.748748',45,1),(23,'2025-12-14 10:55:34.933307','',55000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684524/oiq8k6twwozixnocryvc.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684525/lz2fxudet9noigqgtbij.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684528/wbonpkitseo0vyzdbjiv.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684530/imfhizsipxvdg6fbqyun.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684532/xh3e57sxjasxr8ilfibm.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684534/gj5ci11tnm891ng9hzwt.jpg\"]',_binary '',_binary '','Cơm gà xối mỡ',14,NULL,69000.00,0.00,'2025-12-31 08:44:49.048310',331,1),(24,'2025-12-14 10:56:58.219095','',35000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684617/ewv2sil5d9gxwtz1ybxs.jpg\"]',_binary '',_binary '','Canh rong biển',5,NULL,39000.00,0.00,'2025-12-30 10:42:52.927261',69,1),(25,'2025-12-14 10:57:55.854865','',10000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684675/fllukamvztkgmfsjcocj.jpg\"]',_binary '',_binary '','Nước Vối',4,NULL,10000.00,0.00,'2025-12-24 15:36:33.652785',31,1),(27,'2025-12-14 11:06:54.321882','',69000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685212/sfk8dbch2qctnieepw7f.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685213/t2jrol8hsv7zgzckwgwi.jpg\"]',_binary '',_binary '','Bún Bò Huế',2,NULL,69000.00,0.00,'2025-12-15 08:18:17.699919',651,6),(28,'2025-12-14 11:08:00.399891','',29000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685278/xofb4byau3d6xyapjcwr.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685279/tdjeqdk2rbrx7lclcrtk.jpg\"]',_binary '',_binary '','Bánh Mì Thịt Nướng',3,NULL,29000.00,0.00,'2025-12-14 11:08:00.399891',564,6),(29,'2025-12-14 11:09:32.714749','',6000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685371/diburoxodnx9ijwivdvi.jpg\"]',_binary '',_binary '','Nhân trần',3,NULL,6000.00,0.00,'2025-12-15 08:18:27.277089',2,6),(30,'2025-12-14 11:11:30.248876','',65000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685486/fzu1ngsbbmciynmewfru.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685488/ecgoaavfuvpy1mhjoyti.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765685489/dtnkovofqbjezt8gdli0.webp\"]',_binary '',_binary '','Bún đậu mắm tôm/nước mắm',2,NULL,65000.00,0.00,'2025-12-22 09:11:16.515352',52,6),(31,'2025-12-16 11:26:32.983956','',189000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859189/dkjk8jtfasfyzgxjtasl.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859190/ssoybzz3xdhkgdtuxjru.jpg\"]',_binary '',_binary '','Heo sữa quay',4,0,209000.00,0.00,'2025-12-30 10:42:38.483208',47,8),(32,'2025-12-16 11:28:26.746084','',189000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859305/wirvtfvas9wzgqfniqfd.webp\"]',_binary '',_binary '','Lẩu gà lá é',15,0,229000.00,0.00,'2025-12-30 11:07:05.699687',250,8),(33,'2025-12-16 11:29:41.417623','',69000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859378/wfdwwh8robkciyt9wnec.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859379/jjymrvxlmiuyvaibzjub.jpg\"]',_binary '',_binary '','Khoai lang kén',19,20,89000.00,0.00,'2025-12-31 13:45:09.719640',204,8),(34,'2025-12-16 11:30:56.109621','',49000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg\"]',_binary '',_binary '','Ngô chiên',18,NULL,66000.00,0.00,'2025-12-31 09:11:35.907635',643,8),(35,'2025-12-16 11:32:20.547816','',12000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859538/yfogr8p3aidcjgfir9tr.png\"]',_binary '',_binary '','Bia hơi',7,NULL,12000.00,0.00,'2025-12-30 10:42:48.347038',20,8),(36,'2025-12-16 11:33:54.094020','',79000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859630/yem8ts7shukxtlqvlh05.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859632/bzaphmim6rpbljebnbep.webp\"]',_binary '',_binary '','Đậu tẩm hành',6,15,79000.00,0.00,'2025-12-31 09:11:39.423406',50,8),(37,'2025-12-16 11:35:40.248224','',139000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859736/lsowr2b5vkfsemn2gxg4.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859738/kzvedjdtxl5sywir1pxa.jpg\"]',_binary '',_binary '','Trâu cháy tiêu xanh',3,20,159000.00,0.00,'2025-12-30 10:38:38.299984',51,8),(38,'2025-12-16 11:42:19.828124','',99000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860127/da9j2vqbwjy2juyjox0g.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860129/z9cigkn9svr2hskgg1mo.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860132/yvxp6ip8xewimbhkaf99.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860133/vtl69386dqducmtktgjh.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860137/ux13culxrp7flrjso74v.jpg\"]',_binary '',_binary '','Chả cá lã vọng',2,25,99000.00,0.00,'2025-12-16 11:42:19.828124',1,9),(39,'2025-12-16 11:44:00.849244','',169000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860237/kbvxhgqywntercwwtfzp.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860239/r6zi0qbwjiotyldsbyl9.webp\"]',_binary '',_binary '','Lẩu đầu cá dọc mùng',5,20,189000.00,0.00,'2025-12-30 10:30:29.948498',14,9),(40,'2025-12-16 11:45:24.736465','',59000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860321/n11eegjulndxrcjbjmvb.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765860323/xauvgdmrxeh6qgprk1ns.jpg\"]',_binary '',_binary '','Salad',4,10,59000.00,0.00,'2025-12-30 10:38:22.538169',10,9),(42,'2025-12-14 10:27:22.100014','Siêu to khổng lồ',20000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682832/yxjzuqcziuzv581tgkbx.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682834/z5wv7bl87ery5uc6nttg.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682836/gvc49jovjblcj6nig2sn.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682838/xvf8rapw1phxotajbyr5.png\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682841/ly1gb7takudg14typnes.png\"]',_binary '',_binary '','Bánh tráng trộn siêu cay',1,15,20000.00,0.00,'2025-12-17 10:07:58.274006',10,4),(43,'2025-12-14 10:28:46.489306','',40000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682919/th8z9yxypetaymir3u4a.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682921/si3xtic8lxw2slwxjxhs.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682923/dd7ueivlqb5helzqmm2y.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682924/bhia0ps7asp2glwtk4gb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682925/rednjksrvswzi0cshh5o.jpg\"]',_binary '',_binary '','Cá viên chiên',4,NULL,40000.00,0.00,'2025-12-17 08:29:39.435367',20,4),(44,'2025-12-14 10:30:18.957164','',35000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683011/vjamvlsbax3juuthb07j.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683012/figjv2zxribjvlwtfahy.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683014/v33s5ccwhydpiq6fs1ef.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683016/qcjod5m45uzu4x0vvr4e.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683018/n9e3kaaxvythtjc5epja.jpg\"]',_binary '',_binary '','Sữa tươi trân châu đường đen',1,NULL,35000.00,0.00,'2025-12-15 08:45:42.198733',8,4),(45,'2025-12-14 10:31:50.722656','',55000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683104/t4vadgpmqahihsf66acu.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683106/xbqw6iyg3c0mpqc9jog2.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683107/qfhid2dlaestyllpjhgf.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683108/m5fvuh95yudp7vfrqda3.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765683110/udanxcspjkmksag2edzo.jpg\"]',_binary '',_binary '','Matcha Latte',2,NULL,59000.00,0.00,'2025-12-16 11:17:38.404870',16,4),(46,'2025-12-17 18:25:26.020456','Ngon',89000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765970720/sq3zeffdnjvfrmuludd4.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765970722/gjha0hwvdz91wn5ebefl.webp\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765970724/hf2sbbmefebwbthvumr1.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765970725/qypiwxac4bmxuxippmlx.jpg\"]',_binary '',_binary '','Tobboki phô mai',1,25,109000.00,0.00,'2025-12-30 09:34:59.658407',20,8),(47,'2025-12-16 11:26:32.983956','',189000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859189/dkjk8jtfasfyzgxjtasl.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859190/ssoybzz3xdhkgdtuxjru.jpg\"]',_binary '',_binary '','Heo sữa quay',2,0,209000.00,0.00,'2025-12-22 09:11:09.021003',1,8),(48,'2025-12-16 11:28:26.746084','',189000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg\", \"https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859305/wirvtfvas9wzgqfniqfd.webp\"]',_binary '',_binary '','Lẩu gà lá é',13,0,229000.00,0.00,'2025-12-30 10:42:44.888641',80,8),(53,'2025-12-25 08:28:29.750254','',50000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1766626107/f15w4nj8is8v3swtq56o.webp\"]',_binary '',_binary '','Cơm tấm',1,15,55000.00,0.00,'2025-12-25 10:48:33.807259',6,8),(54,'2025-12-25 08:32:19.046282','',50000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1766626337/ebxl6361lf0zmbphvcow.webp\"]',_binary '\0',_binary '','Cơm tấm',0,15,55000.00,0.00,'2025-12-25 14:34:30.232006',0,8),(55,'2025-12-25 14:35:19.607127','',85000.00,'[\"https://res.cloudinary.com/dxoln0uq3/image/upload/v1766648118/a85qvzitvh0fprwhujoz.webp\"]',_binary '\0',_binary '','Cơm tấm',0,10,100000.00,0.00,'2025-12-25 14:35:25.239775',0,8);
/*!40000 ALTER TABLE `dishes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) DEFAULT NULL,
  `dish_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKg751l1syrkxtg5iamj3rqvjtv` (`dish_id`),
  KEY `FKk7du8b8ewipawnnpg76d55fus` (`user_id`),
  CONSTRAINT `FKg751l1syrkxtg5iamj3rqvjtv` FOREIGN KEY (`dish_id`) REFERENCES `dishes` (`id`),
  CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
INSERT INTO `favorites` VALUES (15,'2025-12-23 16:30:47.280514',33,4);
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchants`
--

DROP TABLE IF EXISTS `merchants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchants` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `close_time` time DEFAULT NULL,
  `commission_rate` decimal(6,5) DEFAULT '0.00001',
  `current_balance` decimal(12,2) DEFAULT '0.00',
  `is_locked` bit(1) NOT NULL,
  `open_time` time DEFAULT NULL,
  `partner_requested_at` datetime(6) DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `restaurant_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `revenue_total` decimal(12,2) DEFAULT '0.00',
  `user_id` bigint NOT NULL,
  `approval_date` datetime(6) DEFAULT NULL,
  `is_approved` bit(1) NOT NULL DEFAULT b'0',
  `locked_at` datetime(6) DEFAULT NULL,
  `registration_date` datetime(6) DEFAULT NULL,
  `rejection_reason` text COLLATE utf8mb4_unicode_ci,
  `status` enum('APPROVED','LOCKED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `partner_status` enum('APPROVED','NONE','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_holder` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKh9v6fyuu5mnk7p3qtlpta2947` (`user_id`),
  UNIQUE KEY `UK2nbsqty98194vs6km7898da52` (`phone`),
  CONSTRAINT `FKa759srj6ts95j9qh089b6gbei` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchants`
--

LOCK TABLES `merchants` WRITE;
/*!40000 ALTER TABLE `merchants` DISABLE KEYS */;
INSERT INTO `merchants` VALUES (1,'Hồ Chí Minh',NULL,'22:00:00',NULL,NULL,_binary '\0','09:00:00',NULL,'0985442161','Cơm Tấm Sài Gòn',NULL,3,'2025-12-14 09:49:52.902206',_binary '',NULL,'2025-12-14 09:49:31.429119','NGON','APPROVED','https://res.cloudinary.com/dxoln0uq3/image/upload/v1766569999/vvikqnvsvwa1chhioxv6.webp',NULL,NULL,NULL,NULL),(2,'120 Hai Bà Trưng',NULL,'22:00:00',NULL,NULL,_binary '\0','08:00:00',NULL,'0985442561','KFC',NULL,4,'2025-12-14 09:52:22.971577',_binary '',NULL,'2025-12-14 09:52:08.155197','KFC','APPROVED','https://res.cloudinary.com/dxoln0uq3/image/upload/v1766378249/hgd7pbpwy98vnfgtwfzg.jpg',NULL,NULL,NULL,NULL),(3,'123 Đường Trần Phú, Vũng Tàu',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442560','Quán Hải Sản Biển Đông',NULL,5,'2025-12-14 09:53:39.294179',_binary '',NULL,'2025-12-14 09:53:22.597822','Hải sản','APPROVED',NULL,NULL,NULL,NULL,NULL),(4,'1 Nguyễn Cơ Thạnh',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442154','Ăn Vặt Tuổi Thơ',NULL,6,'2025-12-14 09:55:23.928378',_binary '',NULL,'2025-12-14 09:55:10.956725','Ăn Vặt Tuổi Thơ','APPROVED',NULL,NULL,NULL,NULL,NULL),(5,'120 Phố Cổ',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442168','Quán Chay An Lạc',NULL,7,'2025-12-14 09:56:33.400215',_binary '',NULL,'2025-12-14 09:56:20.452174','Quán Chay An Lạc','APPROVED',NULL,NULL,NULL,NULL,NULL),(6,'Xuân Phương',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442562','Quán Bún Phở Bánh Mì 24h',NULL,8,'2025-12-14 09:57:48.344379',_binary '',NULL,'2025-12-14 09:57:29.260312','Quán Bún Phở Bánh Mì 24h','APPROVED',NULL,NULL,NULL,NULL,NULL),(8,'HD Mon',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442563','Nhà hàng Hải Xồm',NULL,10,'2025-12-16 11:18:47.196916',_binary '',NULL,'2025-12-16 11:18:23.664853','aaa','APPROVED','https://res.cloudinary.com/dxoln0uq3/image/upload/v1766377159/qodz70grykgrfbmfhiij.jpg','REJECTED',NULL,NULL,NULL),(9,'Lê Đưc Thọ',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985742560','Vua chả cá',NULL,11,'2025-12-16 11:38:07.296577',_binary '',NULL,'2025-12-16 11:37:54.212352','ok\n','APPROVED',NULL,NULL,NULL,NULL,NULL),(24,'Xuân Phương',NULL,NULL,NULL,NULL,_binary '\0',NULL,NULL,'0985442160','Bun\'s Chả',NULL,37,NULL,_binary '\0',NULL,'2025-12-30 15:32:53.527157',NULL,'PENDING',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `merchants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `is_read` bit(1) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('GENERAL','ORDER_CANCELLED','ORDER_COMPLETED','ORDER_CONFIRMED','ORDER_CREATED','ORDER_DELIVERING','ORDER_PREPARING','ORDER_READY','PARTNER_APPROVED','PARTNER_REJECTED','PARTNER_REQUEST','PAYMENT_FAILED','PAYMENT_SUCCESS','PROMOTION','PROMOTION_EXPIRING','PROMOTION_NEW','RECONCILIATION_CLAIM_SUBMITTED','RECONCILIATION_REQUEST_APPROVED','RECONCILIATION_REQUEST_CREATED','RECONCILIATION_REQUEST_REJECTED','REFUND_PROCESSED','SYSTEM','SYSTEM_ANNOUNCEMENT','SYSTEM_MAINTENANCE') NOT NULL,
  `merchant_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7jyhrsvla4aaj01ksj8v3sub3` (`merchant_id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK7jyhrsvla4aaj01ksj8v3sub3` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (16,'Bạn có đơn hàng mới từ user@gmail.com. Tổng giá trị: 44,500 đ. Vui lòng xác nhận đơn hàng.',_binary '\0',NULL,'2025-12-25 14:43:43.308008','Đơn hàng mới #15','ORDER_CREATED',1,3),(27,'Bạn có đơn hàng mới từ user@gmail.com. Tổng giá trị: 90,500 đ. Vui lòng xác nhận đơn hàng.',_binary '\0',NULL,'2025-12-26 15:27:42.300958','Đơn hàng mới #23','ORDER_CREATED',5,7),(98,'Bạn có đơn hàng mới từ user@gmail.com. Tổng giá trị: 89,500 đ. Vui lòng xác nhận đơn hàng.',_binary '','2025-12-31 13:57:09.216880','2025-12-31 13:43:53.973582','Đơn hàng mới #33','ORDER_CREATED',8,10);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dish_id` bigint NOT NULL,
  `dish_image` text,
  `dish_name` varchar(255) NOT NULL,
  `merchant_id` bigint DEFAULT NULL,
  `merchant_name` varchar(255) DEFAULT NULL,
  `quantity` int NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `order_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,22,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684399/zirom3zwy9obuplmbtkk.png','Cơm tấm sườn nướng',1,'Cơm Tấm Sài Gòn',8,392000.00,49000.00,1),(2,24,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684617/ewv2sil5d9gxwtz1ybxs.jpg','Canh rong biển',1,'Cơm Tấm Sài Gòn',3,117000.00,39000.00,2),(3,25,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684675/fllukamvztkgmfsjcocj.jpg','Nước Vối',1,'Cơm Tấm Sài Gòn',2,20000.00,10000.00,2),(4,23,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684524/oiq8k6twwozixnocryvc.jpg','Cơm gà xối mỡ',1,'Cơm Tấm Sài Gòn',1,69000.00,69000.00,3),(5,23,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684524/oiq8k6twwozixnocryvc.jpg','Cơm gà xối mỡ',1,'Cơm Tấm Sài Gòn',7,11483000.00,69000.00,4),(6,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,5),(7,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',2,178000.00,89000.00,5),(8,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,6),(9,53,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1766626107/f15w4nj8is8v3swtq56o.webp','Cơm tấm',8,'Nhà hàng Hải Xồm',1,55000.00,55000.00,7),(13,48,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,11),(14,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,12),(15,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,89000.00,89000.00,13),(16,31,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859189/dkjk8jtfasfyzgxjtasl.jpg','Heo sữa quay',8,'Nhà hàng Hải Xồm',1,209000.00,209000.00,13),(17,35,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859538/yfogr8p3aidcjgfir9tr.png','Bia hơi',8,'Nhà hàng Hải Xồm',1,12000.00,12000.00,13),(18,48,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,14),(19,22,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765684399/zirom3zwy9obuplmbtkk.png','Cơm tấm sườn nướng',1,'Cơm Tấm Sài Gòn',1,49000.00,49000.00,15),(20,48,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,16),(21,37,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859736/lsowr2b5vkfsemn2gxg4.jpg','Trâu cháy tiêu xanh',8,'Nhà hàng Hải Xồm',3,477000.00,159000.00,17),(22,48,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,18),(23,34,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg','Ngô chiên',8,'Nhà hàng Hải Xồm',1,66000.00,66000.00,19),(24,34,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg','Ngô chiên',8,'Nhà hàng Hải Xồm',1,66000.00,66000.00,20),(25,34,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg','Ngô chiên',8,'Nhà hàng Hải Xồm',2,132000.00,66000.00,21),(26,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,89000.00,89000.00,22),(27,48,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,22),(28,6,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765682570/p5h2gvrskwv2y3xrlomy.jpg','Gỏi cuốn chay',5,'Quán Chay An Lạc',1,70000.00,70000.00,23),(29,34,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg','Ngô chiên',8,'Nhà hàng Hải Xồm',1,66000.00,66000.00,24),(30,31,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859189/dkjk8jtfasfyzgxjtasl.jpg','Heo sữa quay',8,'Nhà hàng Hải Xồm',150,31350000.00,209000.00,25),(31,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',150,34350000.00,229000.00,25),(32,36,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859630/yem8ts7shukxtlqvlh05.jpg','Đậu tẩm hành',8,'Nhà hàng Hải Xồm',150,11850000.00,79000.00,25),(33,37,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859736/lsowr2b5vkfsemn2gxg4.jpg','Trâu cháy tiêu xanh',8,'Nhà hàng Hải Xồm',190,30210000.00,159000.00,25),(34,34,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859454/zmv0mnfe3fzdx9mqaqrn.jpg','Ngô chiên',8,'Nhà hàng Hải Xồm',1,66000.00,66000.00,26),(35,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,27),(36,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,89000.00,89000.00,28),(37,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,29),(38,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,89000.00,89000.00,30),(39,32,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859303/oepmlllsxjj7hx9ljbjb.jpg','Lẩu gà lá é',8,'Nhà hàng Hải Xồm',1,229000.00,229000.00,31),(40,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,89000.00,89000.00,32),(41,33,'https://res.cloudinary.com/dxoln0uq3/image/upload/v1765859376/oqfxtdjtb7pf8afcvyfw.jpg','Khoai lang kén',8,'Nhà hàng Hải Xồm',1,69000.00,69000.00,33);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cancellation_reason` text,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `commission_fee` decimal(12,2) DEFAULT NULL,
  `commission_rate` decimal(5,2) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `expected_delivery_time` datetime(6) DEFAULT NULL,
  `items_total` decimal(12,2) DEFAULT NULL,
  `notes` text,
  `order_date` datetime(6) DEFAULT NULL,
  `order_number` varchar(255) NOT NULL,
  `payment_method` enum('CARD','COD') NOT NULL,
  `payment_status` enum('FAILED','PAID','PENDING') NOT NULL,
  `service_fee` decimal(12,2) DEFAULT NULL,
  `shipping_fee` decimal(12,2) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','DELIVERING','PENDING','PROCESSING','READY') NOT NULL,
  `total_amount` decimal(12,2) DEFAULT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `merchant_id` bigint NOT NULL,
  `shipping_address_id` bigint DEFAULT NULL,
  `shipping_partner_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `platform_commission_fee` decimal(12,2) DEFAULT NULL,
  `shipping_commission_fee` decimal(12,2) DEFAULT NULL,
  `cancelled_by` enum('CUSTOMER','MERCHANT') DEFAULT NULL,
  `vnpay_amount` varchar(255) DEFAULT NULL,
  `vnpay_transaction_ref` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnthkiu7pgmnqnu86i2jyoe2v7` (`order_number`),
  KEY `FKn1d1gkxckw648m2n2d5gx0yx5` (`coupon_id`),
  KEY `FKtqvpn5gy0ajx8fip3deoidfxd` (`merchant_id`),
  KEY `FKgvxj7m934s8wmvd6drm7qjdfx` (`shipping_partner_id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKgvxj7m934s8wmvd6drm7qjdfx` FOREIGN KEY (`shipping_partner_id`) REFERENCES `shipping_partners` (`id`),
  CONSTRAINT `FKn1d1gkxckw648m2n2d5gx0yx5` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`),
  CONSTRAINT `FKtqvpn5gy0ajx8fip3deoidfxd` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,NULL,NULL,3075.00,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,392000.00,NULL,'2025-12-24 09:19:39.119178','ORD-20251224-001','COD','PAID',0.00,20500.00,'COMPLETED',412500.00,NULL,1,2,1,2,NULL,NULL,NULL,NULL,NULL),(2,NULL,NULL,3075.00,15.00,'2025-12-26 15:42:17.312878',25000.00,NULL,137000.00,NULL,'2025-12-24 09:23:48.071714','ORD-20251224-002','COD','PAID',0.00,20500.00,'COMPLETED',132500.00,12,1,5,1,12,NULL,NULL,NULL,NULL,NULL),(3,NULL,NULL,3075.00,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,69000.00,NULL,'2025-12-24 09:26:35.749321','ORD-20251224-003','COD','PAID',0.00,20500.00,'COMPLETED',89500.00,NULL,1,6,1,4,NULL,NULL,NULL,NULL,NULL),(4,NULL,'2025-12-24 10:21:21.332065',3075.00,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,1111483000.00,NULL,'2025-12-24 10:20:53.080208','ORD-20251224-004','COD','FAILED',0.00,20500.00,'COMPLETED',503500.00,NULL,1,2,2,2,NULL,NULL,NULL,NULL,NULL),(5,NULL,NULL,3075.00,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,407000.00,NULL,'2025-12-24 15:36:57.928115','ORD-20251224-005','COD','PENDING',0.00,20500.00,'PROCESSING',427500.00,NULL,8,2,2,2,NULL,NULL,NULL,NULL,NULL),(6,'','2025-12-25 08:34:45.804018',3075.00,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-24 16:17:33.121432','ORD-20251224-006','COD','FAILED',0.00,20500.00,'CANCELLED',249500.00,NULL,8,4,2,3,NULL,NULL,NULL,NULL,NULL),(7,'','2025-12-25 11:00:25.698733',NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,55000.00,NULL,'2025-12-25 10:48:33.796287','ORD-20251225-001','COD','FAILED',0.00,20500.00,'CANCELLED',75500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(11,'','2025-12-25 11:42:25.825789',NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-25 11:21:30.793720','ORD-20251225-002','COD','PAID',0.00,20500.00,'COMPLETED',249500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(12,'','2025-12-25 14:28:03.647960',NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-25 11:35:06.280681','ORD-20251225-003','COD','PAID',0.00,20500.00,'COMPLETED',249500.00,NULL,8,2,2,2,NULL,3075.00,'MERCHANT',NULL,NULL),(13,'','2025-12-25 14:27:45.494130',NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,310000.00,NULL,'2025-12-25 11:43:03.835095','ORD-20251225-004','COD','PAID',0.00,20500.00,'COMPLETED',330500.00,NULL,8,2,2,2,NULL,3075.00,'MERCHANT',NULL,NULL),(14,'','2025-12-25 14:26:13.478126',NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-25 13:46:28.151074','ORD-20251225-005','COD','PAID',0.00,20500.00,'COMPLETED',249500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(15,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',25000.00,NULL,49000.00,NULL,'2025-12-25 14:43:43.292387','ORD-20251225-006','COD','PAID',0.00,20500.00,'COMPLETED',44500.00,12,1,2,2,2,NULL,3075.00,NULL,NULL,NULL),(16,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-25 16:34:29.605428','ORD-20251225-007','CARD','PAID',0.00,20500.00,'COMPLETED',249500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(17,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,477000.00,NULL,'2025-12-25 16:48:19.348635','ORD-20251225-008','CARD','PAID',0.00,20500.00,'COMPLETED',497500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(18,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,229000.00,NULL,'2025-12-25 16:54:05.220949','ORD-20251225-009','CARD','PAID',0.00,20500.00,'COMPLETED',249500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(19,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,66000.00,NULL,'2025-12-26 09:09:47.581548','ORD-20251226-001','COD','PAID',0.00,20500.00,'COMPLETED',86500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(20,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,66000.00,NULL,'2025-12-26 09:58:50.216438','ORD-20251226-002','COD','PAID',0.00,20500.00,'COMPLETED',86500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL),(21,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,132000.00,NULL,'2025-12-26 15:24:08.387657','ORD-20251226-003','CARD','PAID',0.00,20500.00,'COMPLETED',152500.00,NULL,8,2,2,2,NULL,3075.00,NULL,'157000','SPY1766737435871'),(22,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,318000.00,NULL,'2025-12-26 15:27:06.061339','ORD-20251226-004','CARD','PAID',0.00,20500.00,'COMPLETED',338500.00,NULL,8,2,2,2,NULL,3075.00,NULL,'343000','SPY1766737613695'),(23,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,70000.00,NULL,'2025-12-26 15:27:42.297760','ORD-20251226-005','CARD','PAID',0.00,20500.00,'COMPLETED',90500.00,NULL,5,2,2,2,NULL,3075.00,NULL,'95000','SPY1766737649815'),(24,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',0.00,NULL,66000.00,NULL,'2025-12-26 15:42:17.312878','ORD-20251226-006','CARD','PAID',0.00,20500.00,'COMPLETED',86500.00,NULL,8,2,2,2,NULL,3075.00,NULL,'91000','SPY1766738524951'),(25,NULL,NULL,NULL,15.00,'2025-12-26 15:42:17.312878',50000.00,NULL,107760000.00,NULL,'2025-12-26 16:45:17.507786','ORD-20251226-007','CARD','PAID',0.00,20500.00,'COMPLETED',107730500.00,19,8,2,2,2,NULL,3075.00,NULL,'107730500','SPY1766742305060'),(26,NULL,NULL,NULL,15.00,NULL,0.00,NULL,66000.00,NULL,'2025-12-26 16:45:47.970787','ORD-20251226-008','CARD','PAID',0.00,22700.00,'PENDING',88700.00,NULL,8,7,2,2,NULL,3405.00,NULL,'88700','SPY1766742335589'),(27,NULL,NULL,NULL,15.00,NULL,0.00,NULL,229000.00,NULL,'2025-12-26 16:46:47.912617','ORD-20251226-009','CARD','PAID',0.00,20500.00,'PENDING',249500.00,NULL,8,2,2,2,NULL,3075.00,NULL,'254000','SPY1766742396310'),(28,NULL,NULL,NULL,15.00,NULL,0.00,NULL,89000.00,NULL,'2025-12-28 14:08:20.859249','ORD-20251228-001','COD','PENDING',0.00,22700.00,'PROCESSING',111700.00,NULL,8,7,2,2,NULL,3405.00,NULL,NULL,NULL),(29,NULL,NULL,NULL,15.00,NULL,0.00,NULL,229000.00,NULL,'2025-12-28 14:34:45.647744','ORD-20251228-002','COD','PENDING',0.00,22700.00,'PROCESSING',251700.00,NULL,8,7,2,2,NULL,3405.00,NULL,NULL,NULL),(30,NULL,NULL,NULL,15.00,NULL,0.00,NULL,89000.00,NULL,'2025-12-28 14:52:22.016559','ORD-20251228-003','COD','PENDING',0.00,22700.00,'PROCESSING',111700.00,NULL,8,7,2,2,NULL,3405.00,NULL,NULL,NULL),(31,NULL,NULL,NULL,15.00,NULL,0.00,NULL,229000.00,NULL,'2025-12-30 11:07:05.675140','ORD-20251230-001','COD','PENDING',0.00,20500.00,'PENDING',249500.00,NULL,8,2,1,2,NULL,3075.00,NULL,NULL,NULL),(32,NULL,NULL,NULL,15.00,NULL,0.00,NULL,89000.00,NULL,'2025-12-30 13:44:06.807591','ORD-20251230-002','CARD','PAID',0.00,20500.00,'PENDING',109500.00,NULL,8,2,1,2,NULL,3075.00,NULL,'114000','SPY1767077040360'),(33,NULL,NULL,NULL,15.00,NULL,0.00,NULL,69000.00,NULL,'2025-12-31 13:43:53.964981','ORD-20251231-001','COD','PENDING',0.00,20500.00,'PENDING',89500.00,NULL,8,2,2,2,NULL,3075.00,NULL,NULL,NULL);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reconciliation_requests`
--

DROP TABLE IF EXISTS `reconciliation_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reconciliation_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `created_at` datetime(6) DEFAULT NULL,
  `merchant_notes` text,
  `net_revenue` decimal(12,2) NOT NULL,
  `platform_commission_rate` decimal(10,8) NOT NULL,
  `rejection_reason` text,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED','REPORTED') NOT NULL,
  `total_gross_revenue` decimal(12,2) NOT NULL,
  `total_orders` int NOT NULL,
  `total_platform_fee` decimal(12,2) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `year_month` varchar(7) NOT NULL,
  `merchant_id` bigint NOT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeqd5byl1dbladootkmumq18y6` (`merchant_id`),
  KEY `FKap00eg69rngrtvg27krc10h66` (`reviewed_by`),
  CONSTRAINT `FKap00eg69rngrtvg27krc10h66` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKeqd5byl1dbladootkmumq18y6` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reconciliation_requests`
--

LOCK TABLES `reconciliation_requests` WRITE;
/*!40000 ALTER TABLE `reconciliation_requests` DISABLE KEYS */;
INSERT INTO `reconciliation_requests` VALUES (1,'','2025-12-28 14:11:36.248688','fs',110288897.10,0.00001000,'s','2025-12-30 08:45:20.908536','REJECTED',110290000.00,13,1102.90,'2025-12-30 08:45:20.908535','2025-12',8,1);
/*!40000 ALTER TABLE `reconciliation_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `revenue_claims`
--

DROP TABLE IF EXISTS `revenue_claims`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revenue_claims` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_response` text,
  `claimed_at` datetime(6) DEFAULT NULL,
  `description` text,
  `disputed_amount` decimal(12,2) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `status` enum('PENDING','RESOLVED','REVIEWED') NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKao0j0qpu94vb9ysisg5ia15pq` (`merchant_id`),
  CONSTRAINT `FKao0j0qpu94vb9ysisg5ia15pq` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `revenue_claims`
--

LOCK TABLES `revenue_claims` WRITE;
/*!40000 ALTER TABLE `revenue_claims` DISABLE KEYS */;
/*!40000 ALTER TABLE `revenue_claims` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shipping_partners`
--

DROP TABLE IF EXISTS `shipping_partners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shipping_partners` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `commission_rate` decimal(4,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_locked` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','PARTNER') NOT NULL,
  `email` varchar(255) NOT NULL,
  `is_default` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shipping_partners`
--

LOCK TABLES `shipping_partners` WRITE;
/*!40000 ALTER TABLE `shipping_partners` DISABLE KEYS */;
INSERT INTO `shipping_partners` VALUES (1,'Hà Tĩnh',15.00,'2025-12-18 15:57:29.124699',_binary '\0','Giao hàng tiết kiệm','0987654331','ACTIVE','ghtk1231@gmail.com',_binary '\0'),(2,'Hà Nội',15.00,'2025-12-18 15:57:45.505185',_binary '\0','Giao hàng nhanh','0985442560','ACTIVE','ghn12312@gmail.com',_binary ''),(3,'Hà Nội',10.00,'2025-12-19 19:00:23.818585',_binary '\0','Viettel Post','0985442571','ACTIVE','viettel12312@gmail.com',_binary '\0');
/*!40000 ALTER TABLE `shipping_partners` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2) NOT NULL,
  `balance_before` decimal(12,2) NOT NULL,
  `notes` text,
  `processed_at` datetime(6) DEFAULT NULL,
  `status` enum('CANCELLED','COMPLETED','PENDING') NOT NULL,
  `transaction_date` datetime(6) DEFAULT NULL,
  `transaction_type` enum('COMMISSION','ORDER_REVENUE','REFUND','WITHDRAWAL') NOT NULL,
  `merchant_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `reconciliation_request_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKosju61fahf0o80fnd5p59jch5` (`merchant_id`),
  KEY `FKfyxndk58yiq2vpn0yd4m09kbt` (`order_id`),
  KEY `FK1w99blvn4mclh7hu3fbui4f02` (`reconciliation_request_id`),
  CONSTRAINT `FK1w99blvn4mclh7hu3fbui4f02` FOREIGN KEY (`reconciliation_request_id`) REFERENCES `reconciliation_requests` (`id`),
  CONSTRAINT `FKfyxndk58yiq2vpn0yd4m09kbt` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKosju61fahf0o80fnd5p59jch5` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transactions`
--

LOCK TABLES `transactions` WRITE;
/*!40000 ALTER TABLE `transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `is_email_verified` bit(1) NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('ADMIN','MERCHANT','USER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verification_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,NULL,'2025-12-14 09:45:14.074131','admin@gmail.com','Nguyễn Anh Tú',_binary '',_binary '','$2a$10$/xm7fsQ0JJaZY7Ki2wWHn.F9L0z8l/8vaJG3NoiGlTSPKJD065IUe',NULL,'ADMIN','2025-12-14 09:45:29.736073',NULL,NULL,NULL),(2,NULL,'2025-12-14 09:46:25.311340','user@gmail.com','Nguyễn Văn A',_binary '',_binary '','$2a$10$2Sn0p4X/wyLqRmjTOQ6MnOiMfuT4jukfKAcuDIOMNUPnf4mNM.th2','0983842179','USER','2025-12-14 09:46:38.676842',NULL,NULL,NULL),(3,NULL,'2025-12-14 09:49:31.426126','comtam@gmail.com','Cơm tấm Sài Gòn',_binary '',_binary '\0','$2a$10$noMkQntVeI7Rw1P6tD.STOvReyGzKCiTxH8.468OoOFtonD6vJpXK','0985442161','MERCHANT','2025-12-14 09:49:31.426126',NULL,NULL,NULL),(4,NULL,'2025-12-14 09:52:08.152631','kfc@gmail.com','KFC',_binary '',_binary '\0','$2a$10$GeR4C8o3kbxM3Gc/3eRY2uVQ.vYUGLb43scPYorLUF.hc1jCl9/EC','0985442561','MERCHANT','2025-12-14 09:52:08.152631',NULL,NULL,NULL),(5,NULL,'2025-12-14 09:53:22.596825','haisan@gmail.com','Quán Hải Sản Biển Đông',_binary '',_binary '\0','$2a$10$kZwUnxx7GotSTmvOyuiAxuBpDDSU0B8aSdzwlvYEKtHm43zRbCD0e','0985442560','MERCHANT','2025-12-14 09:53:22.596825',NULL,NULL,NULL),(6,NULL,'2025-12-14 09:55:10.954694','anvat@gmail.com','Ăn Vặt Tuổi Thơ',_binary '',_binary '\0','$2a$10$AH3gXIfE/Uwb7TiZYDoCW.kqOmqKna7W9dMxCLMe8SXIGL1/7yj5u','0985442154','MERCHANT','2025-12-14 09:55:10.954694',NULL,NULL,NULL),(7,NULL,'2025-12-14 09:56:20.450230','anlac@gmail.com','Quán Chay An Lạc',_binary '',_binary '\0','$2a$10$ANJXz9OjgIFX0e8f8AXBNOvdvR4hMkqPC0VMV635Dll7DwUvnmf2m','0985442168','MERCHANT','2025-12-14 09:56:20.450230',NULL,NULL,NULL),(8,NULL,'2025-12-14 09:57:29.259350','bun@gmail.com','Quán Bún Phở Bánh Mì 24h',_binary '',_binary '\0','$2a$10$xYYABUPh0A6i6GcKvR.KauEHApovH7XelwyMzUKMpt5zBMVb6UCV2','0985442562','MERCHANT','2025-12-14 09:57:29.259350',NULL,NULL,NULL),(10,NULL,'2025-12-16 11:18:23.664853','haixom@gmail.com','Nhà hàng Hải Xồm',_binary '',_binary '\0','$2a$10$dlvMv.kVBhwglHZvVVKTWeegsJFL377oguHkUn4TkU6aWyA5s0SYu','0985442563','MERCHANT','2025-12-16 11:18:23.664853',NULL,NULL,NULL),(11,NULL,'2025-12-16 11:37:54.208844','chaca@gmail.com','Vua chả cá',_binary '',_binary '\0','$2a$10$R/ytiOJoZKzyh32fpfOwXu3cU63xj7dBAcbN.KqNtvrhGwLqcQskS','0985742560','MERCHANT','2025-12-16 11:37:54.208844',NULL,NULL,NULL),(12,NULL,'2025-12-24 09:05:35.094483','no@gmail.com','Nguyễn Anh Tú',_binary '',_binary '','$2a$10$Wf4R8iKHNI5.sCCkVyq34OPTyQ/9SLRCxhWQLuLGATqfcpCjxKJfi','0986341362','USER','2025-12-24 09:05:47.543533',NULL,NULL,NULL),(13,NULL,'2025-12-30 14:07:40.151198','notbot108@gmail.com','Nguyễn Anh Tú',_binary '',_binary '','$2a$10$v/ovLjcl7Zh2.0fqA14DweJ4.sqa3YdaMKW5UTaNfhNQ/g0/Z806u',NULL,'USER','2025-12-30 14:07:54.821317',NULL,NULL,NULL),(37,NULL,'2025-12-30 15:32:53.510451','tu147537@gmail.com',NULL,_binary '',_binary '\0','$2a$10$PO4xtW7yjpsGrmFjdag9LuRKoMhsk8WhlPY3.DlgsG0L5LFw4VXAi','0985442160','MERCHANT','2025-12-30 15:32:53.510451',NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `withdrawal_requests`
--

DROP TABLE IF EXISTS `withdrawal_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `withdrawal_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_notes` text,
  `amount` decimal(12,2) NOT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7bxob10y92tvk407c7rkln47d` (`merchant_id`),
  CONSTRAINT `FK7bxob10y92tvk407c7rkln47d` FOREIGN KEY (`merchant_id`) REFERENCES `merchants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `withdrawal_requests`
--

LOCK TABLES `withdrawal_requests` WRITE;
/*!40000 ALTER TABLE `withdrawal_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `withdrawal_requests` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-02 11:04:54
