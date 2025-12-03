SELECT
 mainPerformanceTable.mainTableId, mainPerformanceTable.companyId, mainPerformanceTable.groupId, mainPerformanceTable.name, mainPerformanceTable.ctCollectionId
FROM
 MainPerformanceTable mainPerformanceTable
WHERE
 mainPerformanceTable.mainPerformanceTableId IN (
  SELECT
   referencePerformanceTable.mainPerformanceTableId
  FROM
   ReferencePerformanceTable referencePerformanceTable
  WHERE
   referencePerformanceTable.name = ?
 )
ORDER BY
 mainPerformanceTable.mainPerformanceTableId ASC