package com.example.demo.api;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
public class DynamicExcelUploadController {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostMapping("/dynamicallyexcel")
	public ResponseEntity<String> uploadExcelFile(@RequestParam("file") MultipartFile file,
			@RequestParam("sheetNumber") int sheetNumber, @RequestParam("rowStart") int rowStart) throws IOException {

		// Read the Excel file
		Workbook workbook = new XSSFWorkbook(file.getInputStream());
		Sheet sheet = workbook.getSheetAt(sheetNumber); // Get the specified sheet
		Iterator<Row> rowIterator = sheet.iterator();

		// Determine the number of columns based on the first row (header row)
		Row headerRow = rowIterator.next(); // Assuming the first row is the header
		List<String> headers = new ArrayList<>();
		for (Cell cell : headerRow) {
			headers.add(cell.getStringCellValue());
		}

		// Dynamically create the table based on the headers
		createTableDynamically(headers);

		// Process the rows starting from the specified rowStart
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Map<String, String> dataMap = processRow(row, headers);

			// Insert the data into the dynamically created table
			insertDataIntoDb(dataMap);
		}

		workbook.close();

		return ResponseEntity.ok("File uploaded and table created successfully!");
	}

	// Create the table dynamically based on the headers
	private void createTableDynamically(List<String> headers) {
		StringBuilder createTableSQL = new StringBuilder(
				"CREATE TABLE IF NOT EXISTS dynamic_table (id SERIAL PRIMARY KEY");

		// Add columns based on the headers
		for (String header : headers) {
			createTableSQL.append(", ").append(header).append(" VARCHAR(255)");
		}

		createTableSQL.append(");");

		// Execute the create table statement
		jdbcTemplate.execute(createTableSQL.toString());
	}

	// Process the row and map it to the headers
	private Map<String, String> processRow(Row row, List<String> headers) {
		Map<String, String> dataMap = new HashMap<>();
		for (int i = 0; i < headers.size(); i++) {
			String key = headers.get(i);
			String value = row.getCell(i) != null ? row.getCell(i).getStringCellValue() : "";
			dataMap.put(key, value);
		}
		return dataMap;
	}

	// Insert data into the dynamically created table
	private void insertDataIntoDb(Map<String, String> dataMap) {
		// Build the INSERT SQL dynamically
		StringBuilder sql = new StringBuilder("INSERT INTO dynamic_table(");
		StringBuilder values = new StringBuilder("VALUES (");

		for (String key : dataMap.keySet()) {
			sql.append(key).append(", ");
			values.append("'").append(dataMap.get(key)).append("', ");
		}

		// Remove the last comma and space
		sql.setLength(sql.length() - 2);
		values.setLength(values.length() - 2);

		sql.append(") ").append(values).append(");");

		// Execute the insert query
		jdbcTemplate.update(sql.toString());
	}
}