package com.pgconnect.management;

import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.property.PublicListingPolicy;
import com.pgconnect.common.AuditLog;
import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import com.pgconnect.inventory.Building;
import com.pgconnect.inventory.BuildingRepository;
import com.pgconnect.tenancy.Tenancy;
import com.pgconnect.tenancy.TenancyRepository;
import com.pgconnect.tenancy.TenancyStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ManagementService {
    private final ComplaintRepository complaints; private final AnnouncementRepository announcements; private final FoodMenuRepository menus;
    private final NotificationRepository notifications; private final ReviewRepository reviews; private final DepositRepository deposits;
    private final TenancyRepository tenancies; private final PgPropertyRepository properties; private final UserAccountRepository users; private final BuildingRepository buildings; private final MongoTemplate mongo;
    public ManagementService(ComplaintRepository complaints,AnnouncementRepository announcements,FoodMenuRepository menus,NotificationRepository notifications,
                            ReviewRepository reviews,DepositRepository deposits,TenancyRepository tenancies,PgPropertyRepository properties,
                            UserAccountRepository users,BuildingRepository buildings,MongoTemplate mongo){this.complaints=complaints;this.announcements=announcements;this.menus=menus;this.notifications=notifications;this.reviews=reviews;this.deposits=deposits;this.tenancies=tenancies;this.properties=properties;this.users=users;this.buildings=buildings;this.mongo=mongo;}

    public List<Complaint> residentComplaints(String residentId){return complaints.findByResidentIdOrderByCreatedAtDesc(residentId);}
    @org.springframework.transaction.annotation.Transactional
    public Complaint createComplaint(String residentId,ManagementController.ComplaintRequest request){
        Tenancy tenancy=tenancies.findFirstByResidentIdAndStatusInOrderByStartDateDesc(residentId,List.of(TenancyStatus.ACTIVE,TenancyStatus.MOVE_OUT_REQUESTED))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"You need an active accommodation before submitting a property complaint"));
        Complaint c=new Complaint();c.setResidentId(residentId);c.setOwnerId(tenancy.getOwnerId());c.setTenancyId(tenancy.getId());c.setPropertyId(tenancy.getPropertyId());c.setBuildingId(tenancy.getBuildingId());c.setRoomId(tenancy.getRoomId());c.setBedId(tenancy.getBedId());c.setCategory(request.category().trim());c.setTitle(request.title().trim());c.setDescription(request.description().trim());
        Complaint saved=complaints.save(c);notify(tenancy.getOwnerId(),"COMPLAINT","New maintenance request",request.title(),"Complaint",saved.getId());return saved;
    }
    public List<Complaint> ownerComplaints(String ownerId,String propertyId){requireOwned(ownerId,propertyId);return complaints.findByOwnerIdAndPropertyIdOrderByCreatedAtDesc(ownerId,propertyId);}
    @org.springframework.transaction.annotation.Transactional
    public Complaint updateComplaint(String ownerId,String propertyId,String id,ManagementController.ComplaintUpdate request){
        return updateComplaint(ownerId,ownerId,propertyId,id,request);
    }
    @org.springframework.transaction.annotation.Transactional
    public Complaint updateComplaint(String actorId,String ownerId,String propertyId,String id,ManagementController.ComplaintUpdate request){
        Complaint c=complaints.findByIdAndOwnerIdAndPropertyId(id,ownerId,propertyId).orElseThrow(()->notFound("Complaint not found"));
        if(c.getStatus()==ComplaintStatus.CLOSED && request.status()!=ComplaintStatus.CLOSED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Closed complaints cannot be reopened");
        c.setStatus(request.status());c.setOwnerResponse(request.response()==null?null:request.response().trim());c.setAssignedTo(request.assignedTo()==null?null:request.assignedTo().trim());c.setUpdatedAt(Instant.now());if(request.status()==ComplaintStatus.RESOLVED||request.status()==ComplaintStatus.CLOSED)c.setResolvedAt(Instant.now());
        Complaint saved=complaints.save(c);notify(c.getResidentId(),"COMPLAINT_UPDATE","Maintenance request updated",c.getTitle()+" · "+request.status().name().replace('_',' '),"Complaint",c.getId());
        mongo.save(new AuditLog(actorId,"COMPLAINT_STATUS_UPDATED","Complaint",id,Map.of("status",request.status().name())));return saved;
    }

    public List<Announcement> ownerAnnouncements(String ownerId,String propertyId){requireOwned(ownerId,propertyId);return announcements.findByOwnerIdAndPropertyIdOrderByPublishedAtDesc(ownerId,propertyId);}
    @org.springframework.transaction.annotation.Transactional
    public Announcement createAnnouncement(String ownerId,String propertyId,ManagementController.AnnouncementRequest request){
        return createAnnouncement(ownerId,ownerId,propertyId,request);
    }
    @org.springframework.transaction.annotation.Transactional
    public Announcement createAnnouncement(String actorId,String ownerId,String propertyId,ManagementController.AnnouncementRequest request){
        requireOwned(ownerId,propertyId);if(request.expiresAt()!=null&&request.expiresAt().isBefore(Instant.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Announcement expiry must be in the future");
        if(request.buildingId()!=null)buildings.findById(request.buildingId()).filter(b->b.getOwnerId().equals(ownerId)&&b.getPropertyId().equals(propertyId)).orElseThrow(()->notFound("Building not found"));
        Tenancy residentTenancy=null;
        if(request.residentId()!=null){residentTenancy=activeTenancies(request.residentId()).stream().filter(t->t.getOwnerId().equals(ownerId)&&t.getPropertyId().equals(propertyId)).findFirst().orElseThrow(()->notFound("Resident is not associated with this property"));
            if(request.buildingId()!=null&&!request.buildingId().equals(residentTenancy.getBuildingId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The selected resident is not in this building");}
        Announcement a=new Announcement();a.setOwnerId(ownerId);a.setPropertyId(propertyId);a.setBuildingId(request.buildingId()!=null?request.buildingId():residentTenancy==null?null:residentTenancy.getBuildingId());a.setResidentId(request.residentId());a.setTitle(request.title().trim());a.setBody(request.body().trim());a.setExpiresAt(request.expiresAt());
        Announcement saved=announcements.save(a);
        mongo.save(new AuditLog(actorId,"ANNOUNCEMENT_CREATED","Announcement",saved.getId(),Map.of("propertyId",propertyId)));
        if(request.residentId()!=null)notify(request.residentId(),"ANNOUNCEMENT",saved.getTitle(),saved.getBody(),"Announcement",saved.getId());
        else tenancies.findByOwnerIdAndPropertyIdAndStatusIn(ownerId,propertyId,List.of(TenancyStatus.ACTIVE,TenancyStatus.MOVE_OUT_REQUESTED)).stream().map(Tenancy::getResidentId).distinct().forEach(id->notify(id,"ANNOUNCEMENT",saved.getTitle(),saved.getBody(),"Announcement",saved.getId()));
        return saved;
    }
    public void deleteAnnouncement(String ownerId,String propertyId,String id){deleteAnnouncement(ownerId,ownerId,propertyId,id);}
    @org.springframework.transaction.annotation.Transactional
    public void deleteAnnouncement(String actorId,String ownerId,String propertyId,String id){Announcement a=announcements.findById(id).filter(v->v.getOwnerId().equals(ownerId)&&v.getPropertyId().equals(propertyId)).orElseThrow(()->notFound("Announcement not found"));announcements.delete(a);mongo.save(new AuditLog(actorId,"ANNOUNCEMENT_DELETED","Announcement",id,Map.of("propertyId",propertyId)));}
    public List<Announcement> residentAnnouncements(String residentId){
        List<Tenancy> active=tenancies.findByResidentIdOrderByStartDateDesc(residentId).stream().filter(t->t.getStatus()==TenancyStatus.ACTIVE||t.getStatus()==TenancyStatus.MOVE_OUT_REQUESTED).toList();
        Instant now=Instant.now();return active.stream().flatMap(t->announcements.findByPropertyIdAndPublishedTrueOrderByPublishedAtDesc(t.getPropertyId()).stream())
                .filter(a->a.getResidentId()==null||a.getResidentId().equals(residentId)).filter(a->a.getExpiresAt()==null||a.getExpiresAt().isAfter(now))
                .filter(a->a.getBuildingId()==null||active.stream().anyMatch(t->t.getPropertyId().equals(a.getPropertyId())&&t.getBuildingId().equals(a.getBuildingId())))
                .distinct().sorted((x,y)->y.getPublishedAt().compareTo(x.getPublishedAt())).toList();
    }

    public List<ManagementController.FoodMenuView> ownerMenu(String ownerId,String propertyId){requireOwned(ownerId,propertyId);return menus.findByOwnerIdAndPropertyIdOrderByDayOfWeekAsc(ownerId,propertyId).stream().map(this::menuView).toList();}
    public List<ManagementController.FoodMenuView> residentMenu(String residentId){return activeTenancies(residentId).stream().flatMap(t->menus.findByPropertyIdAndPublishedTrueOrderByDayOfWeekAsc(t.getPropertyId()).stream()).map(this::menuView).toList();}
    public ManagementController.FoodMenuView saveMenu(String ownerId,String propertyId,ManagementController.MenuRequest request){
        return saveMenu(ownerId,ownerId,propertyId,request);
    }
    @org.springframework.transaction.annotation.Transactional
    public ManagementController.FoodMenuView saveMenu(String actorId,String ownerId,String propertyId,ManagementController.MenuRequest request){
        requireOwned(ownerId,propertyId);String meal=request.meal().trim().toUpperCase();if(!List.of("BREAKFAST","LUNCH","EVENING_SNACKS","DINNER").contains(meal))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose breakfast, lunch, evening snacks or dinner");
        FoodMenu menu=menus.findByOwnerIdAndPropertyIdAndDayOfWeekAndMeal(ownerId,propertyId,request.dayOfWeek(),meal).orElseGet(FoodMenu::new);
        menu.setOwnerId(ownerId);menu.setPropertyId(propertyId);menu.setDayOfWeek(request.dayOfWeek());menu.setMeal(meal);menu.setDescription(request.description().trim());menu.setVegetarianOption(trim(request.vegetarianOption()));menu.setNonVegetarianOption(trim(request.nonVegetarianOption()));menu.setMealTime(trim(request.mealTime()));menu.setPublished(request.published());
        FoodMenu saved=menus.save(menu);mongo.save(new AuditLog(actorId,"FOOD_MENU_UPDATED","FoodMenu",saved.getId(),Map.of("propertyId",propertyId,"dayOfWeek",request.dayOfWeek().toString(),"meal",meal)));return menuView(saved);
    }

    public List<ResidentNotification> notifications(String userId){return notifications.findTop100ByUserIdOrderByCreatedAtDesc(userId);}
    public ResidentNotification markNotificationRead(String userId,String id){
        Query q=Query.query(Criteria.where("_id").is(id).and("userId").is(userId));ResidentNotification changed=mongo.findAndModify(q,new Update().set("readAt",Instant.now()),ResidentNotification.class);
        if(changed==null)throw notFound("Notification not found");changed.setReadAt(Instant.now());return changed;
    }
    public ReviewSummary publicReviews(String propertyId){
        PgProperty property=properties.findById(propertyId).orElseThrow(()->notFound("Property not found"));
        UserAccount owner=users.findById(property.getOwnerId()).orElse(null);
        if(!PublicListingPolicy.isEligible(property,owner))throw notFound("Property not found");
        List<PropertyReview> published=reviews.findByPropertyIdAndStatusOrderByCreatedAtDesc(propertyId,"PUBLISHED");double average=published.stream().mapToInt(PropertyReview::getRating).average().orElse(0);
        List<PublicReview> publicReviews=published.stream().limit(20).map(r->new PublicReview(r.getId(),r.getRating(),r.getText(),r.getCreatedAt())).toList();
        return new ReviewSummary(published.size(),Math.round(average*10d)/10d,publicReviews);
    }
    public PropertyReview createReview(String residentId,String propertyId,ManagementController.ReviewRequest request){
        Tenancy stay=tenancies.findByResidentIdOrderByStartDateDesc(residentId).stream().filter(t->t.getPropertyId().equals(propertyId)&&(t.getStatus()==TenancyStatus.ENDED||t.getStatus()==TenancyStatus.ACTIVE||t.getStatus()==TenancyStatus.MOVE_OUT_REQUESTED)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"A verified stay at this property is required to review it"));
        PropertyReview r=new PropertyReview();r.setResidentId(residentId);r.setOwnerId(stay.getOwnerId());r.setPropertyId(propertyId);r.setTenancyId(stay.getId());r.setRating(request.rating());r.setText(request.text().trim());
        try{return reviews.save(r);}catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"You have already reviewed this property");}
    }
    public PropertyReview reportReview(String residentId,String id,ManagementController.ReviewReport request){
        PropertyReview r=reviews.findById(id).orElseThrow(()->notFound("Review not found"));if(r.getResidentId().equals(residentId))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cannot report your own review");
        r.setStatus("REPORTED");r.setReportReason(request.reason().trim());r.setUpdatedAt(Instant.now());return reviews.save(r);
    }

    public List<Deposit> deposits(String ownerId,String propertyId){requireOwned(ownerId,propertyId);return deposits.findByOwnerIdAndPropertyIdOrderByReceivedAtDesc(ownerId,propertyId);}
    @org.springframework.transaction.annotation.Transactional
    public Deposit recordDeposit(String ownerId,String propertyId,ManagementController.DepositRequest request){
        Tenancy t=tenancies.findByIdAndOwnerIdAndPropertyId(request.tenancyId(),ownerId,propertyId).orElseThrow(()->notFound("Tenancy not found"));
        if(t.getStatus()==TenancyStatus.ENDED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot record a new deposit on an ended tenancy");
        Deposit d=new Deposit();d.setOwnerId(ownerId);d.setResidentId(t.getResidentId());d.setTenancyId(t.getId());d.setPropertyId(propertyId);d.setAmount(request.amount());d.setReference(trim(request.reference()));
        Deposit saved=deposits.save(d);mongo.save(new AuditLog(ownerId,"DEPOSIT_RECORDED","Deposit",saved.getId(),Map.of("amount",Long.toString(saved.getAmount()),"tenancyId",t.getId())));return saved;
    }
    @org.springframework.transaction.annotation.Transactional
    public Deposit refundDeposit(String ownerId,String propertyId,String id,ManagementController.RefundRequest request){
        Deposit d=deposits.findById(id).filter(v->v.getOwnerId().equals(ownerId)&&v.getPropertyId().equals(propertyId)).orElseThrow(()->notFound("Deposit not found"));
        Query q=Query.query(Criteria.where("_id").is(id).and("ownerId").is(ownerId).and("amount").gte(request.amount()).and("refundedAmount").lte(d.getAmount()-request.amount()));
        Deposit changed=mongo.findAndModify(q,new Update().inc("refundedAmount",request.amount()).set("updatedAt",Instant.now()),Deposit.class);
        if(changed==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Refund exceeds the remaining deposit. Refresh and retry.");
        long total=d.getRefundedAmount()+request.amount();changed.setRefundedAmount(total);changed.setStatus(total>=d.getAmount()?"REFUNDED":"PARTIALLY_REFUNDED");changed.setUpdatedAt(Instant.now());changed=deposits.save(changed);
        mongo.save(new AuditLog(ownerId,"DEPOSIT_REFUND_RECORDED","Deposit",id,Map.of("amount",Long.toString(request.amount()),"reason",request.reason())));return changed;
    }

    private List<Tenancy> activeTenancies(String residentId){return tenancies.findByResidentIdOrderByStartDateDesc(residentId).stream().filter(t->t.getStatus()==TenancyStatus.ACTIVE||t.getStatus()==TenancyStatus.MOVE_OUT_REQUESTED).toList();}
    private ManagementController.FoodMenuView menuView(FoodMenu m){return new ManagementController.FoodMenuView(m.getId(),m.getPropertyId(),m.getDayOfWeek(),m.getMeal(),m.getDescription(),m.getVegetarianOption(),m.getNonVegetarianOption(),m.getMealTime(),m.isPublished());}
    private void notify(String user,String type,String title,String message,String entityType,String entityId){ResidentNotification n=new ResidentNotification();n.setUserId(user);n.setType(type);n.setTitle(title);n.setMessage(message);n.setEntityType(entityType);n.setEntityId(entityId);notifications.save(n);}
    private void requireOwned(String owner,String property){properties.findByIdAndOwnerId(property,owner).orElseThrow(()->notFound("Property not found"));}
    private static String trim(String s){return s==null||s.isBlank()?null:s.trim();}
    private static ResponseStatusException notFound(String message){return new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
    public record PublicReview(String id,int rating,String text,Instant createdAt){}
    public record ReviewSummary(long count,double average,List<PublicReview> reviews){}
}
