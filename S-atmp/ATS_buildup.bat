@echo off & setlocal enabledelayedexpansion
rem 1.拷贝需要打包的文件到目标文件夹
set /P version=input new build version:
mkdir D:\buildup\ATS-%version%
set destdir=D:\buildup\ATS-%version%

xcopy /V /I /E AtmpLogFiles %destdir%\AtmpLogFiles 
xcopy /V /I /E lib %destdir%\lib
xcopy /V /I /E src\testScripts %destdir%\src\testScripts
xcopy /V /I /E sysinfo %destdir%\sysinfo
xcopy /V /I /E TestConfig %destdir%\TestConfig
xcopy /V /I /E TestReports %destdir%\TestReports
xcopy /V /I /E .project %destdir%\.project
xcopy /V /I /E .classpath %destdir%\.classpath

rem 2.清除残留的日志和报告文件，清除atmp文件夹
cd %destdir%
del /S /Q AtmpLogFiles\*.log*
del /Q TestReports\*.*
rd /S /Q src\atmp

rem 3.清理TEC配置文件中的密码信息
cd sysinfo\sysconfig
for /f "tokens=*" %%i in (atmp_config.xml) do ( 
   if "%%i"=="" (echo.) else (set "line=%%i" & call :chg)
)>> atmp_config1.xml
copy /Y atmp_config1.xml atmp_config.xml
del /F atmp_config1.xml
exit
:chg
set "line=!line:Gsqlgdlkl2016=!"
set "line=!line:lihao=!"
echo !line!
goto :eof