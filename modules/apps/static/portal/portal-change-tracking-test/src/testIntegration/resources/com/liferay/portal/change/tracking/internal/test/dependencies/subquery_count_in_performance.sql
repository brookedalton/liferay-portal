SELECT
 COUNT(*)
FROM
 MainPerformanceTable
WHERE
 MainPerformanceTable.mainPerformanceTableId IN (
  SELECT
   mainPerformanceTableId
  FROM
   ReferencePerformanceTable
  WHERE
   ReferencePerformanceTable.name = ?
 )