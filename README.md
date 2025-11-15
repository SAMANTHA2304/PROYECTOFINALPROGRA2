# Proyecto Biblioteca (Swing + MySQL)
NetBeans/Maven • Capas UI/Service/DAO/Model • JDBC (PreparedStatement) • BCrypt.

## Requisitos
- JDK 17+
- MySQL en `localhost:3306`
- NetBeans (o IntelliJ/Eclipse)
- Credenciales en `src/main/resources/db.properties`

## Instalación
1) Importa proyecto Maven en NetBeans.  
2) Ejecuta `schema.sql` en MySQL (Workbench/CLI).  
3) Edita `db.properties` con tu usuario/clave.  
4) Run: clase `com.example.app.App`.

### Login
- Usuario: `admin`
- Contraseña: `admin123`

## Empaquetar .jar
```bash
mvn -q clean package
java -jar target/swing-biblioteca-1.0.0-jar-with-dependencies.jar
```
