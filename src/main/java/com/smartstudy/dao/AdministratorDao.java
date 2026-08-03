package com.smartstudy.dao;
import com.smartstudy.config.Database; import com.smartstudy.model.Administrator;
import java.sql.*; import java.util.Optional;
public final class AdministratorDao {
    public Optional<Administrator> findByEmail(String email) throws SQLException { try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM administrators WHERE LOWER(email)=LOWER(?)")){p.setString(1,email);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(new Administrator(r.getInt("admin_id"),r.getString("full_name"),r.getString("email"),r.getString("password_hash"))):Optional.empty();}} }
}
