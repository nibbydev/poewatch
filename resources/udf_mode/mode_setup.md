All files related to the MySQL mode UDF are created by [Robert Eisele](http://www.xarg.org/), originate from https://github.com/infusion/udf_infusion/ and are licenced under the GPL Version 2 license.

1. Install prequisites
    `apt install gcc libmysqlclient-dev build-essential`
2. Locate MySQL lib files
    e.g `/usr/include/mysql`
3. Compile .c file
    `gcc -shared -o stats_mode.so stats_mode.c -I/usr/include/mysql -m64 -lstdc++ -fPIC`
4. Find plugin dir and copy compiled file
    `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"`
    e.g `/usr/lib/mysql/plugin/`
5. Add function to MySQL
    `CREATE AGGREGATE FUNCTION stats_mode RETURNS REAL SONAME 'stats_mode.so';`