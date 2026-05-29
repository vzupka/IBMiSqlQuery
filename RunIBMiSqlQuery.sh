echo ===========================================================================
echo --- Create executable Jar file IBMiSqlQuery.jar
echo ===========================================================================

echo The following command gets the directory in which the script is placed

script_dir=$(dirname $0)
echo script_dir=$(dirname $0)
echo $script_dir

echo -------------------------------------------------------------
echo The following command makes the application directory current

cd $script_dir
echo cd $script_dir

echo -------------------------------------------------------------------
echo The following command creates the Jar file in the current directory

echo jar cvfm  IBMiSqlQuery.jar  manifestIBMiSqlQuery.txt  -C build/classes queries/Q_MainWindow.class  -C build/classes queries -C build/classes locales
jar cvfm  IBMiSqlQuery.jar  manifestIBMiSqlQuery.txt  -C build/classes queries/Q_MainWindow.class  -C build/classes queries -C build/classes locales

echo -------------------------------------------
echo The following command executes the Jar file

echo java -jar IBMiSqlQuery.jar
java -jar IBMiSqlQuery.jar