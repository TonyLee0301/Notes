CREATE TABLE `user` (
    `id` int(11) NOT NULL auto_increment,
    `name` varchar(255) default NULL,
    `age` int(11) default NULL,
    `sex` varchar(10) default NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;