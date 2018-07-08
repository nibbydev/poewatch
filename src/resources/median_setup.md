1. Install prequisites
    `apt install gcc libmysqlclient-dev build-essential`
2. compile .cc file
    `gcc -shared -o udf_median.so udf_median.cc -I/usr/include/mysql -m64 -lstdc++ -fPIC`
3. Find plugin dir
    `SHOW VARIABLES WHERE Variable_Name LIKE "%dir"`
    `/usr/lib/mysql/plugin/`
4. Copy .so there
5. Add function to MySQL
    `CREATE AGGREGATE FUNCTION median RETURNS REAL SONAME 'udf_median.so';`