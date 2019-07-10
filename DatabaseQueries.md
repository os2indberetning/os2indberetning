1) Liste over ledere/godkendere, der har fravalgt at få mails fra OS2indberetning om godkendelse af kørsler
```
SELECT
*
FROM (SELECT DISTINCT 
`empl`.`Id`, 
`p`.`FirstName`,
`p`.`LastName`,
`p`.`Initials`,
`empl`.`EmploymentId`, 
`empl`.`Position`, 
`empl`.`IsLeader`, 
`empl`.`StartDateTimestamp`, 
`empl`.`EndDateTimestamp`, 
`empl`.`PersonId`, 
`empl`.`OrgUnitId`

FROM `Employments` AS `empl` 
INNER JOIN `Substitutes` AS `sub` 
ON `empl`.`PersonId` = `sub`.`SubId` OR `empl`.`IsLeader`
INNER JOIN `people` AS `p` 
ON `empl`.`PersonId` = `p`.`Id` AND NOT `p`.`RecieveMail`) AS `q`

```