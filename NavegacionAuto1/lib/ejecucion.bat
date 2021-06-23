@echo Directorio principal
cd C:\DATOS\GITHUB_REPOS\repo_java\NavegacionAuto1\

@echo Compilando el JAR con Maven (modo batch)
call mvn clean compile assembly:single -B

@echo Ejecutando la clase MAIN
java -cp C:\DATOS\GITHUB_REPOS\repo_java\NavegacionAuto1\target\navegacionauto1-0.0.1-SNAPSHOT-jar-with-dependencies.jar controladoresselenium.ordenador.NavegarAuto

pause



