namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class EmploymentIdDatatypeChange : DbMigration
    {
        public override void Up()
        {
            AlterColumn("Employments", "EmploymentId", c => c.String(nullable: false, unicode: false));
            DropColumn("Employments", "ServiceNumber");
        }
        
        public override void Down()
        {
            AddColumn("Employments", "ServiceNumber", c => c.String(unicode: false));
            AlterColumn("Employments", "EmploymentId", c => c.Int(nullable: false));
        }
    }
}
