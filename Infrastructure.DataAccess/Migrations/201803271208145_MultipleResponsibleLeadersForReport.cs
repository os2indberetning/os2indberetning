namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class MultipleResponsibleLeadersForReport : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("Reports", "ResponsibleLeaderId", "People");
            DropIndex("Reports", new[] { "ResponsibleLeaderId" });
            AddColumn("People", "Report_Id", c => c.Int());
            CreateIndex("People", "Report_Id");
            AddForeignKey("People", "Report_Id", "Reports", "Id");
            DropColumn("Reports", "ResponsibleLeaderId");
        }
        
        public override void Down()
        {
            AddColumn("Reports", "ResponsibleLeaderId", c => c.Int());
            DropForeignKey("People", "Report_Id", "Reports");
            DropIndex("People", new[] { "Report_Id" });
            DropColumn("People", "Report_Id");
            CreateIndex("Reports", "ResponsibleLeaderId");
            AddForeignKey("Reports", "ResponsibleLeaderId", "People", "Id");
        }
    }
}
