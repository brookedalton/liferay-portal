SELECT
 mainPerformanceTable.mainPerformanceTableId, mainPerformanceTable.companyId, mainPerformanceTable.groupId, mainPerformanceTable.name, mainPerformanceTable.ctCollectionId
FROM
 MainPerformanceTable mainPerformanceTable
WHERE
 mainPerformanceTable.groupId = ?
ORDER BY
 mainPerformanceTable.mainPerformanceTableId ASC