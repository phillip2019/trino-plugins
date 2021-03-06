# Spreadsheet Storage Handler

This storage handler allow a user to place spreadsheets (.xlsx format) in their Hdfs home directory and then perform SQL queries against them via Trino.

### Building

Maven package command:

    mvn package
    
The current project is based on Hadoop CD5, if you want a specific version of Hadoop use the `-Dhadoop.version=` propery to specify.

    mvn package -Dhadoop.version=2.6.0

After cloning and building extract the `spreadsheet-storage-handler-*-plugin.tar.gz` into the plugin directory in your Trino installation.

Next configure your spreadsheet catalog by adding a `spreadsheet.properties` file in the `etc/catalog` directory in your Trino installation.

#### Example spreadsheet.properties file:

    connector.name=spreadsheet
    basepath=hdfs://<namenode>/user
    subdir=spreadsheets

#### Usage

Next place your desired spreadsheet in your home directory.  For example if your username is `user1` then you would need to place the spreadsheet in the `hdfs://<namenode>/user/user1/spreadsheets` directory.  Once the file is in place a new schema in the spreadsheet catalog will appear.  Each sheet in the spreadsheet will be represented as table in Trino.

#### Schema & Table Mapping

The following XLSX example [Trino Example.xlsx](
https://docs.google.com/spreadsheets/d/1I708PZJDYvtTouQWhC4kXjxAAB04nnpkmdwEZ6MMuec/edit#gid=0 "Trino Example.xlsx") has two sheets:

- Simple Sheet
- Multiple Types Per Column

This will produce a new shema of `trino_example_xlsx` with two tables:
- simple_sheet
- multiple_types_per_column

##### Trino Representaion

###### Connect with Trino CLI

    java -jar trino-cli-*-executable.jar --catalog spreadsheet --user user1

###### Show Schemas

    trino:default> show schemas;
            Schema         
    -----------------------
    trino_example_xlsx
    (1 row)

###### Show Tables

    trino:default> use trino_example_xlsx;
    trino:trino_example_xlsx> show tables;
               Table           
    ---------------------------
    multiple_types_per_column 
    simple_sheet              
    (2 rows)

###### Select Data - Simple Sheet

    trino:trino_example_xlsx> select * from simple_sheet;
     a |  b  | c | d  
    ---+-----+---+----
     a | 1.0 | 2 | 3  
     b | 2.0 | 3 | 5  
     c | 3.0 | 4 | 7  
     d | 4.0 | 5 | 9  
     e | 5.0 | 6 | 11 
     f | 6.0 | 7 | 13 
    (6 rows)

###### Select Data - Multiple Types Per Column
    trino:trino_example_xlsx> select * from multiple_types_per_column;
     a_number | a_string | b_boolean | b_number | b_string 
    ----------+----------+-----------+----------+----------
     NULL     | test1    | NULL      | NULL     | NULL     
     NULL     | NULL     | NULL      | NULL     | test2    
      12345.0 | NULL     | true      | NULL     | NULL     
     NULL     | NULL     | NULL      |      1.5 | NULL     
    (4 rows)

#### Type Mapping

Spreadsheets are very flexible allowing multiple types per column, however this flexiblity does not easily translate to a relational table.  This plugin handles this impedance mismatch by examining each cell in the column.  If there only one type (String, Boolean, Double) is detected the column is labeled the same name as the spreadsheet column (A, B, C, etc).  However if multiple types are detected then a column for each type is created (A_NUMBER, A_STRING, etc) and the cells are null for the values that are a different type.  For example:

##### Spreadsheet
| |A|B|C|D|
|---|---|:---:|---|---|
|1|abc|3|TRUE|d|
|2|123|4|FALSE|   |
|3|true|5|TRUE|g|

Would translate to:

##### Trino Table
|a_boolean|a_number|a_string|b|c|d|
|---|---|---|:---:|---|---|
|NULL|NULL|abc|3|TRUE|d|
|NULL|123|NULL|4|FALSE|NULL|
|true|NULL|NULL|5|TRUE|g|

