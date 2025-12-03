SELECT
 COUNT(*)
FROM
 MainPerformanceTable
INNER JOIN
 ReferencePerformanceTable
ON
 ReferencePerformanceTable.mainPerformanceTableId = MainPerformanceTable.mainPerformanceTableId
WHERE
 ReferencePerformanceTable.name = ?