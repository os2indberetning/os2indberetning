namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class MultipleResponsibleLeadersForReport : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("Reports", "ResponsibleLeaderId", "People");
            //DropForeignKey("Reports", "Person_Id", "People");
            //DropIndex("Reports", new[] { "Person_Id" });
            DropIndex("Reports", new[] { "ResponsibleLeaderId" });
            //DropColumn("Reports", "Person_Id");

            CreateTable(
                "ReportPersonMapping",
                c => new
                    {
                        Id = c.Int(identity: true),
                        ReportId = c.Int(nullable: false),
                        PersonId = c.Int(nullable: false),
                    })
                .PrimaryKey(t => new { t.Id, t.ReportId, t.PersonId })                
                .ForeignKey("Reports", t => t.ReportId, cascadeDelete: true)
                .ForeignKey("People", t => t.PersonId, cascadeDelete: true)
                .Index(t => t.ReportId)
                .Index(t => t.PersonId);

            Sql("INSERT INTO ReportPersonMapping (ReportId, PersonId) SELECT Id, ResponsibleLeaderId FROM Reports");

            DropColumn("Reports", "ResponsibleLeaderId");
        }
        
        public override void Down()
        {
            AddColumn("Reports", "ResponsibleLeaderId", c => c.Int());
            CreateIndex("Reports", "ResponsibleLeaderId");
            AddForeignKey("Reports", "ResponsibleLeaderId", "People", "Id");

            DropForeignKey("ReportPersonMapping", "PersonId", "People");
            DropForeignKey("ReportPersonMapping", "ReportId", "Reports");
            DropIndex("ReportPersonMapping", new[] { "PersonId" });
            DropIndex("ReportPersonMapping", new[] { "ReportId" });
            DropTable("ReportPersonMapping");

            Sql("UPDATE Reports SET ResponsibleLeaderId = ActualLeaderId");
            //CreateIndex("People", "Report_Id");
            //AddForeignKey("People", "Report_Id", "Reports", "Id");
        }
    }
}
