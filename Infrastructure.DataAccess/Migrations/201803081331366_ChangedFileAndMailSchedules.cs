namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class ChangedFileAndMailSchedules : DbMigration
    {
        public override void Up()
        {
            AddColumn("FileGenerationSchedules", "Repeat", c => c.Boolean(nullable: false));
            AddColumn("MailNotificationSchedules", "FileGenerationScheduleId", c => c.Int(nullable: false));
            AddColumn("MailNotificationSchedules", "CustomText", c => c.String(unicode: false));
            CreateIndex("MailNotificationSchedules", "FileGenerationScheduleId");
            AddForeignKey("MailNotificationSchedules", "FileGenerationScheduleId", "FileGenerationSchedules", "Id", cascadeDelete: true);
            DropColumn("FileGenerationSchedules", "Generated");
            DropColumn("MailNotificationSchedules", "PayRoleTimestamp");
            DropColumn("MailNotificationSchedules", "Notified");
            DropColumn("MailNotificationSchedules", "Repeat");
        }
        
        public override void Down()
        {
            AddColumn("MailNotificationSchedules", "Repeat", c => c.Boolean(nullable: false));
            AddColumn("MailNotificationSchedules", "Notified", c => c.Boolean(nullable: false));
            AddColumn("MailNotificationSchedules", "PayRoleTimestamp", c => c.Long(nullable: false));
            AddColumn("FileGenerationSchedules", "Generated", c => c.Boolean(nullable: false));
            DropForeignKey("MailNotificationSchedules", "FileGenerationScheduleId", "FileGenerationSchedules");
            DropIndex("MailNotificationSchedules", new[] { "FileGenerationScheduleId" });
            DropColumn("MailNotificationSchedules", "CustomText");
            DropColumn("MailNotificationSchedules", "FileGenerationScheduleId");
            DropColumn("FileGenerationSchedules", "Repeat");
        }
    }
}
