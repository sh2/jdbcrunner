package jdbcrunner;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

import jdbcrunner.Template.DataType;

public class TemplateTest {
	public static final String JDBC_URL = "jdbc:mysql://k02c7/jdbcrunner?useSSL=false&allowPublicKeyRetrieval=true";
	public static final String JDBC_USER = "jdbcrunner";
	public static final String JDBC_PASS = "jdbcrunner";

	@Test
	public void testTemplate() {
		Template template;

		template = new Template("SELECT ename FROM emp");
		assertEquals("SELECT ename FROM emp", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 0);

		template = new Template("SELECT ename FROM emp WHERE empno = $int");
		assertEquals("SELECT ename FROM emp WHERE empno = ?", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 1);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.INT));

		template = new Template("SELECT ename FROM emp WHERE empno = $long");
		assertEquals("SELECT ename FROM emp WHERE empno = ?", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 1);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.LONG));

		template = new Template("SELECT ename FROM emp WHERE empno = $double");
		assertEquals("SELECT ename FROM emp WHERE empno = ?", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 1);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.DOUBLE));

		template = new Template("SELECT ename FROM emp WHERE ename = $string");
		assertEquals("SELECT ename FROM emp WHERE ename = ?", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 1);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.STRING));

		template = new Template("SELECT ename FROM emp WHERE hiredate = $timestamp");
		assertEquals("SELECT ename FROM emp WHERE hiredate = ?", template.getPreparableStatement());
		assertTrue(template.getDataTypeList().size() == 1);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.TIMESTAMP));

		template = new Template("SELECT ename FROM emp WHERE empno = $int and ename = $string");

		assertEquals("SELECT ename FROM emp WHERE empno = ? and ename = ?",
				template.getPreparableStatement());

		assertTrue(template.getDataTypeList().size() == 2);
		assertTrue(template.getDataTypeList().get(0).equals(DataType.INT));
		assertTrue(template.getDataTypeList().get(1).equals(DataType.STRING));

		template = new Template("SELECT ename FROM emp WHERE ename = 'scott$$tiger'");

		assertEquals("SELECT ename FROM emp WHERE ename = 'scott$tiger'",
				template.getPreparableStatement());

		assertTrue(template.getDataTypeList().size() == 0);

		try {
			template = new Template("SELECT ename FROM emp "
					+ "WHERE empno = $error and ename = $string");

			fail("IllegalArgumentException was not thrown");
		} catch (IllegalArgumentException e) {
			assertEquals(Resources.getString("Template.ILLEGAL_PARAMETER_NAME")
					+ "$error and ename = ", e.getMessage());
		}

		try {
			template = new Template("SELECT ename FROM emp WHERE ename = 'scott$tiger'");
			fail("IllegalArgumentException was not thrown");
		} catch (IllegalArgumentException e) {
			assertEquals(Resources.getString("Template.ILLEGAL_PARAMETER_NAME") + "$tiger'",
					e.getMessage());
		}
	}

	@Test
	public void testSetParameters01() {
		Connection connection = null;
		Statement statement = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
			connection.setAutoCommit(false);
			statement = connection.createStatement();
			statement.execute("DROP TABLE IF EXISTS test01");

			statement.execute("CREATE TABLE test01 "
					+ "(id INT PRIMARY KEY, data1 BIGINT, data2 VARCHAR(100))");

			statement.execute("INSERT INTO test01 (id, data1, data2) "
					+ "VALUES (1, 10000000000, 'abc')");

			statement.execute("INSERT INTO test01 (id, data1, data2) "
					+ "VALUES (2, 20000000000, 'def')");

			statement.execute("INSERT INTO test01 (id, data1, data2) "
					+ "VALUES (3, 30000000000, 'ghi')");

			statement.execute("INSERT INTO test01 (id, data1, data2) VALUES (4, NULL, NULL)");
			connection.commit();

			Context context = ContextFactory.getGlobal().enterContext();
			Scriptable scope = new ImporterTopLevel(context);

			context.evaluateString(scope, "function setParameters() "
					+ "{ template.setParameters(preparedStatement, Array.slice(arguments, 0)); }",
					"", 1, null);

			Template template = null;
			template = new Template("SELECT id, data1, data2 FROM test01 WHERE id = $int");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(0);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("abc", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(5 / 2);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("def", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(\"03\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("ghi", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(4);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getString(3));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			try {
				context.evaluateString(scope, "setParameters(\"a\");", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_STRING_FOR_INT_1") + "1"
						+ Resources.getString("Template.ILLEGAL_STRING_FOR_INT_2") + "a",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(new java.util.Date());", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_NUMBER_1") + "1"
								+ Resources.getString("Template.ILLEGAL_NUMBER_2")
								+ "java.util.Date", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters();", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "0"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(1, 2);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "2"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			preparedStatement.close();

			template = new Template("SELECT id, data1, data2 FROM test01 WHERE data1 = $long");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(0);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(10000000000);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("abc", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(40000000001 / 2);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("def", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(\"030000000000\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("ghi", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			try {
				context.evaluateString(scope, "setParameters(\"a\");", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_1") + "1"
						+ Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_2") + "a",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(new java.util.Date());", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_NUMBER_1") + "1"
								+ Resources.getString("Template.ILLEGAL_NUMBER_2")
								+ "java.util.Date", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters();", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "0"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(1, 2);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "2"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			preparedStatement.close();

			template = new Template("SELECT id, data1, data2 FROM test01 WHERE data1 = $double");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(0);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(10000000000);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("abc", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			// This test passes on MySQL 5.5.21 or
			// later. http://bugs.mysql.com/50756
			context.evaluateString(scope, "setParameters(40000000001 / 2);", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(\"030000000000\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals("ghi", resultSet.getString(3));
			assertFalse(resultSet.next());
			resultSet.close();

			try {
				context.evaluateString(scope, "setParameters(\"a\");", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_STRING_FOR_DOUBLE_1") + "1"
						+ Resources.getString("Template.ILLEGAL_STRING_FOR_DOUBLE_2") + "a",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(new java.util.Date());", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_NUMBER_1") + "1"
								+ Resources.getString("Template.ILLEGAL_NUMBER_2")
								+ "java.util.Date", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters();", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "0"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(1, 2);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "2"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			preparedStatement.close();

			template = new Template("SELECT id, data1, data2 FROM test01 WHERE data2 = $string");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(\"xyz\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(\"abc\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals(10000000000L, resultSet.getLong(2));
			assertFalse(resultSet.next());
			resultSet.close();

			try {
				context.evaluateString(scope, "setParameters(1);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_STRING_1") + "1"
								+ Resources.getString("Template.ILLEGAL_STRING_2")
								+ "java.lang.Double", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters();", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "0"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(\"def\", \"ghi\");", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "2"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "1",
						we.getMessage());
			}

			preparedStatement.close();

			template = new Template(
					"SELECT id, data1, data2 FROM test01 WHERE data1 = $long and data2 = $string");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(10000000000, \"xyz\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(0, \"abc\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertFalse(resultSet.next());
			resultSet.close();

			context.evaluateString(scope, "setParameters(10000000000, \"abc\");", "", 1, null);
			resultSet = preparedStatement.executeQuery();
			assertTrue(resultSet.next());
			assertEquals(1, resultSet.getInt(1));
			assertFalse(resultSet.next());
			resultSet.close();

			try {
				context.evaluateString(scope, "setParameters(\"abc\", \"abc\");", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_1") + "1"
						+ Resources.getString("Template.ILLEGAL_STRING_FOR_LONG_2") + "abc",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(10000000000, 100);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_STRING_1") + "2"
								+ Resources.getString("Template.ILLEGAL_STRING_2")
								+ "java.lang.Double", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(10000000000, new java.util.Date());",
						"", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(
						Resources.getString("Template.ILLEGAL_STRING_1") + "2"
								+ Resources.getString("Template.ILLEGAL_STRING_2")
								+ "java.util.Date", we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(10000000000);", "", 1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "1"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "2",
						we.getMessage());
			}

			try {
				context.evaluateString(scope, "setParameters(10000000000, \"abc\", \"def\");", "",
						1, null);
				fail("WrappedException was not thrown");
			} catch (WrappedException e) {
				Throwable we = e.getWrappedException();
				assertEquals(Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_1") + "3"
						+ Resources.getString("Template.ILLEGAL_NUMBER_OF_PARAMETERS_2") + "2",
						we.getMessage());
			}
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			try {
				Context.exit();
			} catch (IllegalStateException e) {
				// NOOP
			}
		}
	}

	@Test
	public void testSetParameters02() {
		Connection connection = null;
		Statement statement = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		int count = 0;

		try {
			connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);

			Context context = ContextFactory.getGlobal().enterContext();
			Scriptable scope = new ImporterTopLevel(context);
			context.evaluateString(scope, "function setParameters() "
					+ "{ template.setParameters(preparedStatement, Array.slice(arguments, 0)); }",
					"", 1, null);

			// INT -> INT
			statement = connection.createStatement();
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 INT)");

			Template template = null;
			template = new Template("INSERT INTO test02 VALUES ($int)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1, resultSet.getInt(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0, resultSet.getInt(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// INT -> BIGINT
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 BIGINT)");

			template = new Template("INSERT INTO test02 VALUES ($int)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1L, resultSet.getLong(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0L, resultSet.getLong(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// INT -> DOUBLE
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DOUBLE)");

			template = new Template("INSERT INTO test02 VALUES ($int)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1.0, resultSet.getDouble(1), 0.0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0.0, resultSet.getDouble(1), 0.0);
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// INT -> DECIMAL
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DECIMAL(8, 4))");

			template = new Template("INSERT INTO test02 VALUES ($int)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getBigDecimal(1).compareTo(new BigDecimal(1)) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getBigDecimal(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// LONG -> INT
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 INT)");

			template = new Template("INSERT INTO test02 VALUES ($long)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1, resultSet.getInt(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0, resultSet.getInt(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// LONG -> BIGINT
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 BIGINT)");

			template = new Template("INSERT INTO test02 VALUES ($long)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1L, resultSet.getLong(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0L, resultSet.getLong(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// LONG -> DOUBLE
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DOUBLE)");

			template = new Template("INSERT INTO test02 VALUES ($long)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1.0, resultSet.getDouble(1), 0.0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0.0, resultSet.getDouble(1), 0.0);
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// LONG -> DECIMAL
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DECIMAL(8, 4))");

			template = new Template("INSERT INTO test02 VALUES ($long)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getBigDecimal(1).compareTo(new BigDecimal(1)) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getBigDecimal(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// DOUBLE -> INT
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 INT)");

			template = new Template("INSERT INTO test02 VALUES ($double)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1.5);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(2, resultSet.getInt(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0, resultSet.getInt(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// DOUBLE -> BIGINT
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 BIGINT)");

			template = new Template("INSERT INTO test02 VALUES ($double)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1.5);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(2L, resultSet.getLong(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0L, resultSet.getLong(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// DOUBLE -> DOUBLE
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DOUBLE)");

			template = new Template("INSERT INTO test02 VALUES ($double)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1.5);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(1.5, resultSet.getDouble(1), 0.0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(0.0, resultSet.getDouble(1), 0.0);
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// DOUBLE -> DECIMAL
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DECIMAL(8, 4))");

			template = new Template("INSERT INTO test02 VALUES ($double)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(1.5);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getBigDecimal(1).compareTo(new BigDecimal(1.5)) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getBigDecimal(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// STRING -> VARCHAR
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 VARCHAR(10))");

			template = new Template("INSERT INTO test02 VALUES ($string)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(\"abc\");", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals("abc", resultSet.getString(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getString(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// STRING -> CHAR
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 CHAR(10))");

			template = new Template("INSERT INTO test02 VALUES ($string)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);

			context.evaluateString(scope, "setParameters(\"abc\");", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals("abc", resultSet.getString(1));
			assertFalse(resultSet.next());
			resultSet.close();

			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getString(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// TIMESTAMP -> DATETIME
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DATETIME)");

			template = new Template("INSERT INTO test02 VALUES ($timestamp)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);
			Timestamp correctTimestamp = Timestamp.valueOf("2010-01-02 03:04:05");

			// 1. JavaScript Date
			context.evaluateString(scope, "setParameters(new Date(2010, 0, 2, 3, 4, 5));", "", 1,
					null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTimestamp(1).compareTo(correctTimestamp) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 2. Java Date
			statement.executeUpdate("DELETE FROM test02");

			context.evaluateString(scope, "setParameters(new java.util.Date(1262369045000));", "",
					1, null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTimestamp(1).compareTo(correctTimestamp) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 3. Number
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(1262369045000);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTimestamp(1).compareTo(correctTimestamp) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 4. String
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(\"2010-01-02 03:04:05\");", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTimestamp(1).compareTo(correctTimestamp) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 5. Null
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getTimestamp(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// TIMESTAMP -> DATE
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 DATE)");

			template = new Template("INSERT INTO test02 VALUES ($timestamp)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);
			Date correctDate = Date.valueOf("2010-01-02");

			// 1. JavaScript Date
			context.evaluateString(scope, "setParameters(new Date(2010, 0, 2, 3, 4, 5));", "", 1,
					null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getDate(1).compareTo(correctDate) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 2. Java Date
			statement.executeUpdate("DELETE FROM test02");

			context.evaluateString(scope, "setParameters(new java.util.Date(1262369045000));", "",
					1, null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getDate(1).compareTo(correctDate) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 3. Number
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(1262369045000);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getDate(1).compareTo(correctDate) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 4. String
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(\"2010-01-02 03:04:05\");", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getDate(1).compareTo(correctDate) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 5. Null
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getDate(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();

			// TIMESTAMP -> TIME
			statement.execute("DROP TABLE IF EXISTS test02");
			statement.execute("CREATE TABLE test02 (c1 TIME)");

			template = new Template("INSERT INTO test02 VALUES ($timestamp)");
			preparedStatement = connection.prepareStatement(template.getPreparableStatement());
			scope.put("template", scope, template);
			scope.put("preparedStatement", scope, preparedStatement);
			Time correctTime = Time.valueOf("03:04:05");

			// 1. JavaScript Date
			context.evaluateString(scope, "setParameters(new Date(2010, 0, 2, 3, 4, 5));", "", 1,
					null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTime(1).compareTo(correctTime) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 2. Java Date
			statement.executeUpdate("DELETE FROM test02");

			context.evaluateString(scope, "setParameters(new java.util.Date(1262369045000));", "",
					1, null);

			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTime(1).compareTo(correctTime) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 3. Number
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(1262369045000);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTime(1).compareTo(correctTime) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 4. String
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(\"2010-01-02 03:04:05\");", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertTrue(resultSet.getTime(1).compareTo(correctTime) == 0);
			assertFalse(resultSet.next());
			resultSet.close();

			// 5. Null
			statement.executeUpdate("DELETE FROM test02");
			context.evaluateString(scope, "setParameters(null);", "", 1, null);
			count = preparedStatement.executeUpdate();
			assertEquals(1, count);

			resultSet = statement.executeQuery("SELECT c1 FROM test02");
			assertTrue(resultSet.next());
			assertEquals(null, resultSet.getTime(1));
			assertTrue(resultSet.wasNull());
			assertFalse(resultSet.next());
			resultSet.close();

			preparedStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (preparedStatement != null) {
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// NOOP
				}
			}

			try {
				Context.exit();
			} catch (IllegalStateException e) {
				// NOOP
			}
		}
	}
}
