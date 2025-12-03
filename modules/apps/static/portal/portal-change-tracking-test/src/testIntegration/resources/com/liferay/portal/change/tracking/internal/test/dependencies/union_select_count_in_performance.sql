(
 SELECT
  COUNT(*)
 FROM
  MainPerformanceTable
)
UNION ALL
(
 SELECT
  COUNT(*)
 FROM
  ReferencePerformanceTable
)