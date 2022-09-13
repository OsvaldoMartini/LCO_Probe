## SonarQube

### LCO_Probe
```
mvn clean verify sonar:sonar \
-Dsonar.projectKey=LCO_Probe \
-Dsonar.host.url=http://localhost:9000 \
-Dsonar.login=sqp_816179acf70c63b70af8b98c6356a46e989e4822
```


## Maven Build

### With *maven-assembly-plugin*
```
mvn clean install   (Fat jar file)
```

### With *spring-boot-maven-plugin*
```
mvn clean package spring-boot:repackage
```