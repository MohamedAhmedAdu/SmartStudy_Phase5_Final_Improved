package com.smartstudy.dao;
import com.smartstudy.config.Database; import com.smartstudy.model.*;
import java.sql.*; import java.time.LocalDateTime; import java.util.*;
public final class TaskDao {
    private AcademicTask map(ResultSet r)throws SQLException{return new AcademicTask(r.getInt("task_id"),r.getString("title"),r.getTimestamp("due_date")==null?null:r.getTimestamp("due_date").toLocalDateTime(),r.getDouble("grade_weight"),r.getDouble("estimated_hours"),TaskStatus.valueOf(r.getString("status")),TaskType.valueOf(r.getString("task_type")),r.getString("submit_type"),(Boolean)r.getObject("allow_late"),r.getString("location"),(Integer)r.getObject("duration_min"),(Boolean)r.getObject("is_online"),(Integer)r.getObject("attempts"),r.getInt("course_id"));}
    public List<AcademicTask> findByCourse(int cid)throws SQLException{List<AcademicTask>o=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM tasks WHERE course_id=? ORDER BY due_date")){p.setInt(1,cid);try(ResultSet r=p.executeQuery()){while(r.next())o.add(map(r));}}return o;}
    public List<AcademicTask> findByStudent(int sid)throws SQLException{List<AcademicTask>o=new ArrayList<>();String q="SELECT t.* FROM tasks t JOIN courses c ON c.course_id=t.course_id WHERE c.student_id=? ORDER BY t.due_date";try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setInt(1,sid);try(ResultSet r=p.executeQuery()){while(r.next())o.add(map(r));}}return o;}
    public AcademicTask insert(AcademicTask x)throws SQLException{String q="INSERT INTO tasks(title,due_date,grade_weight,estimated_hours,status,task_type,submit_type,allow_late,location,duration_min,is_online,attempts,course_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q,Statement.RETURN_GENERATED_KEYS)){bind(p,x);p.executeUpdate();try(ResultSet k=p.getGeneratedKeys()){k.next();return new AcademicTask(k.getInt(1),x.title(),x.dueDate(),x.gradeWeight(),x.estimatedHours(),x.status(),x.taskType(),x.submitType(),x.allowLate(),x.location(),x.durationMin(),x.online(),x.attempts(),x.courseId());}}}
    public void update(AcademicTask x)throws SQLException{String q="UPDATE tasks SET title=?,due_date=?,grade_weight=?,estimated_hours=?,status=?,task_type=?,submit_type=?,allow_late=?,location=?,duration_min=?,is_online=?,attempts=? WHERE task_id=? AND course_id=?";try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setString(1,x.title());if(x.dueDate()==null)p.setNull(2,Types.TIMESTAMP);else p.setTimestamp(2,Timestamp.valueOf(x.dueDate()));p.setDouble(3,x.gradeWeight());p.setDouble(4,x.estimatedHours());p.setString(5,x.status().name());p.setString(6,x.taskType().name());p.setString(7,x.submitType());setObj(p,8,x.allowLate(),Types.BOOLEAN);p.setString(9,x.location());setObj(p,10,x.durationMin(),Types.INTEGER);setObj(p,11,x.online(),Types.BOOLEAN);setObj(p,12,x.attempts(),Types.INTEGER);p.setInt(13,x.taskId());p.setInt(14,x.courseId());p.executeUpdate();}}
    public void updateStatus(int id,TaskStatus s)throws SQLException{try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE tasks SET status=? WHERE task_id=?")){p.setString(1,s.name());p.setInt(2,id);p.executeUpdate();}}
    public void delete(int id)throws SQLException{try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM tasks WHERE task_id=?")){p.setInt(1,id);p.executeUpdate();}}

    public boolean existsEquivalent(int courseId, String title, LocalDateTime dueDate) throws SQLException {
        String query = "SELECT 1 FROM tasks WHERE course_id=? AND LOWER(title)=LOWER(?) AND due_date=? LIMIT 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            statement.setString(2, title);
            statement.setTimestamp(3, Timestamp.valueOf(dueDate));
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
    private void bind(PreparedStatement p,AcademicTask x)throws SQLException{p.setString(1,x.title());if(x.dueDate()==null)p.setNull(2,Types.TIMESTAMP);else p.setTimestamp(2,Timestamp.valueOf(x.dueDate()));p.setDouble(3,x.gradeWeight());p.setDouble(4,x.estimatedHours());p.setString(5,x.status().name());p.setString(6,x.taskType().name());p.setString(7,x.submitType());setObj(p,8,x.allowLate(),Types.BOOLEAN);p.setString(9,x.location());setObj(p,10,x.durationMin(),Types.INTEGER);setObj(p,11,x.online(),Types.BOOLEAN);setObj(p,12,x.attempts(),Types.INTEGER);p.setInt(13,x.courseId());}
    private void setObj(PreparedStatement p,int i,Object v,int t)throws SQLException{if(v==null)p.setNull(i,t);else p.setObject(i,v);}
}
