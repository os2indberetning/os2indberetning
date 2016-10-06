namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class VacationModule : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("Reports", "EmploymentId", "Employments");
            DropForeignKey("Reports", "ActualLeaderId", "People");
            DropForeignKey("Reports", "ApprovedById", "People");
            DropForeignKey("Reports", "Person_Id", "People");
            DropForeignKey("Reports", "ResponsibleLeaderId", "People");
            DropForeignKey("Addresses", "DriveReportId", "Reports");
            DropIndex("Reports", new[] { "ActualLeaderId" });
            DropIndex("Reports", new[] { "ApprovedById" });
            DropIndex("Reports", new[] { "EmploymentId" });
            DropIndex("Reports", new[] { "PersonId" });
            DropIndex("Reports", new[] { "ResponsibleLeaderId" });
            DropIndex("Reports", new[] { "Person_Id" });
            RenameTable(name: "Reports", newName: "DriveReports");
            CreateTable(
                "VacationBalances",
                c => new
                    {
                        Id = c.Int(nullable: false, identity: true),
                        Year = c.Int(nullable: false),
                        TotalVacationHours = c.Double(nullable: false),
                        VacationHours = c.Double(nullable: false),
                        TransferredHours = c.Double(nullable: false),
                        FreeVacationHours = c.Double(nullable: false),
                        UpdatedAt = c.Long(nullable: false),
                        EmploymentId = c.Int(nullable: false),
                        PersonId = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.Id)                
                .ForeignKey("Employments", t => t.EmploymentId, cascadeDelete: true)
                .ForeignKey("People", t => t.PersonId, cascadeDelete: true)
                .Index(t => t.EmploymentId)
                .Index(t => t.PersonId);
            
            CreateTable(
                "VacationReports",
                c => new
                    {
                        Id = c.Int(nullable: false, identity: true),
                        Status = c.Int(nullable: false),
                        CreatedDateTimestamp = c.Long(nullable: false),
                        EditedDateTimestamp = c.Long(nullable: false),
                        Comment = c.String(nullable: false, unicode: false),
                        ClosedDateTimestamp = c.Long(nullable: false),
                        ProcessedDateTimestamp = c.Long(nullable: false),
                        ApprovedById = c.Int(),
                        PersonId = c.Int(nullable: false),
                        EmploymentId = c.Int(nullable: false),
                        ResponsibleLeaderId = c.Int(),
                        ActualLeaderId = c.Int(),
                        StartTimestamp = c.Long(nullable: false),
                        StartTime = c.Time(precision: 0),
                        EndTimestamp = c.Long(nullable: false),
                        EndTime = c.Time(precision: 0),
                        Purpose = c.String(unicode: false),
                        VacationYear = c.Int(nullable: false),
                        VacationHours = c.Int(nullable: false),
                        VacationType = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.Id)                
                .ForeignKey("People", t => t.ApprovedById)
                .ForeignKey("People", t => t.PersonId, cascadeDelete: true)
                .ForeignKey("Employments", t => t.EmploymentId, cascadeDelete: true)
                .ForeignKey("People", t => t.ResponsibleLeaderId)
                .ForeignKey("People", t => t.ActualLeaderId)
                .Index(t => t.ApprovedById)
                .Index(t => t.PersonId)
                .Index(t => t.EmploymentId)
                .Index(t => t.ResponsibleLeaderId)
                .Index(t => t.ActualLeaderId);
            
            AddColumn("OrgUnits", "HasAccessToVacation", c => c.Boolean(nullable: false));
            AddColumn("Substitutes", "Type", c => c.Int(nullable: false));
            CreateIndex("DriveReports", "Person_Id");
            CreateIndex("DriveReports", "ApprovedById");
            CreateIndex("DriveReports", "PersonId");
            CreateIndex("DriveReports", "EmploymentId");
            CreateIndex("DriveReports", "ResponsibleLeaderId");
            CreateIndex("DriveReports", "ActualLeaderId");
            AddForeignKey("DriveReports", "EmploymentId", "Employments", "Id");
            AddForeignKey("DriveReports", "ActualLeaderId", "People", "Id");
            AddForeignKey("DriveReports", "ApprovedById", "People", "Id");
            AddForeignKey("DriveReports", "Person_Id", "People", "Id");
            AddForeignKey("DriveReports", "ResponsibleLeaderId", "People", "Id");
            AddForeignKey("Addresses", "DriveReportId", "DriveReports", "Id");
            DropColumn("DriveReports", "Discriminator");
        }
        
        public override void Down()
        {
            AddColumn("DriveReports", "Discriminator", c => c.String(nullable: false, maxLength: 128, storeType: "nvarchar"));
            DropForeignKey("DriveReports", "EmploymentId", "Employments");
            DropForeignKey("DriveReports", "ActualLeaderId", "People");
            DropForeignKey("DriveReports", "ApprovedById", "People");
            DropForeignKey("DriveReports", "Person_Id", "People");
            DropForeignKey("DriveReports", "ResponsibleLeaderId", "People");
            DropForeignKey("Addresses", "DriveReportId", "DriveReports");
            DropForeignKey("VacationReports", "ActualLeaderId", "People");
            DropForeignKey("VacationReports", "ResponsibleLeaderId", "People");
            DropForeignKey("VacationReports", "EmploymentId", "Employments");
            DropForeignKey("VacationReports", "PersonId", "People");
            DropForeignKey("VacationReports", "ApprovedById", "People");
            DropForeignKey("VacationBalances", "PersonId", "People");
            DropForeignKey("VacationBalances", "EmploymentId", "Employments");
            DropIndex("VacationReports", new[] { "ActualLeaderId" });
            DropIndex("VacationReports", new[] { "ResponsibleLeaderId" });
            DropIndex("VacationReports", new[] { "EmploymentId" });
            DropIndex("VacationReports", new[] { "PersonId" });
            DropIndex("VacationReports", new[] { "ApprovedById" });
            DropIndex("DriveReports", new[] { "ActualLeaderId" });
            DropIndex("DriveReports", new[] { "ResponsibleLeaderId" });
            DropIndex("DriveReports", new[] { "EmploymentId" });
            DropIndex("DriveReports", new[] { "PersonId" });
            DropIndex("DriveReports", new[] { "ApprovedById" });
            DropIndex("DriveReports", new[] { "Person_Id" });
            DropIndex("VacationBalances", new[] { "PersonId" });
            DropIndex("VacationBalances", new[] { "EmploymentId" });
            DropColumn("Substitutes", "Type");
            DropColumn("OrgUnits", "HasAccessToVacation");
            DropTable("VacationReports");
            DropTable("VacationBalances");
            CreateIndex("Reports", "Person_Id");
            CreateIndex("Reports", "ResponsibleLeaderId");
            CreateIndex("Reports", "PersonId");
            CreateIndex("Reports", "EmploymentId");
            CreateIndex("Reports", "ApprovedById");
            CreateIndex("Reports", "ActualLeaderId");
            AddForeignKey("Addresses", "DriveReportId", "Reports", "Id", cascadeDelete: true);
            AddForeignKey("Reports", "EmploymentId", "Employments", "Id");
            AddForeignKey("Reports", "ActualLeaderId", "People", "Id");
            AddForeignKey("Reports", "ApprovedById", "People", "Id");
            AddForeignKey("Reports", "Person_Id", "People", "Id");
            AddForeignKey("Reports", "ResponsibleLeaderId", "People", "Id");
            RenameTable(name: "DriveReports", newName: "Reports");
        }
    }
}
