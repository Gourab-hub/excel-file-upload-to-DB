package com.example.demo.api;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
public class ExcelUploadController {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostMapping("/fileUpload")
	public ResponseEntity<String> uploadExcelFile(@RequestParam("fileName") MultipartFile file,
			@RequestParam("sheetNumber") int sheetNumber, @RequestParam("rowNumber") int rowStart,
			@RequestParam("keys") List<String> keys) throws IOException {

		// Read the Excel file
		Workbook workbook = new XSSFWorkbook(file.getInputStream());
		Sheet sheet = workbook.getSheetAt(sheetNumber); // Get the specified sheet
		Iterator<Row> rowIterator = sheet.iterator();

		// Skip rows until the starting row
		int rowCount = 0;
		while (rowIterator.hasNext() && rowCount < rowStart) {
			rowIterator.next();
			rowCount++;
		}

		// Process the rows starting from the specified rowStart
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Map<String, String> dataMap = processRow(row, keys);

			// Insert the data into the database
			insertDataIntoDb(dataMap);
		}

		workbook.close();

		return ResponseEntity.ok("File uploaded successfully!");
	}

	private Map<String, String> processRow(Row row, List<String> keys) {
		Map<String, String> dataMap = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = row.getCell(i).getStringCellValue(); // Get value from cell
			dataMap.put(key, value);
		}
		return dataMap;
	}

	private void insertDataIntoDb(Map<String, String> dataMap) {
		String sql = "INSERT INTO public.student(name, city) VALUES (?, ?)";
		jdbcTemplate.update(sql, dataMap.get("name"), dataMap.get("city"));
	}
}
