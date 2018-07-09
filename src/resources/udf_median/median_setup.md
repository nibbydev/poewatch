All files related to the MySQL median UDF originate from http://mysql-udf.sourceforge.net/ and are licenced under the [2-Clause BSD License](https://opensource.org/licenses/bsd-license.php)

1. Install prequisites
    `apt install gcc libmysqlclient-dev build-essential`
2. Locate MySQL lib files
    e.g `/usr/include/mysql`
3. Compile .cc file
    `gcc -shared -o udf_median.so udf_median.cc -I/usr/include/mysql -m64 -lstdc++ -fPIC`
4. Find plugin dir and copy compiled file
    `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"`
    e.g `/usr/lib/mysql/plugin/`
5. Add function to MySQL
    `CREATE AGGREGATE FUNCTION median RETURNS REAL SONAME 'udf_median.so';`