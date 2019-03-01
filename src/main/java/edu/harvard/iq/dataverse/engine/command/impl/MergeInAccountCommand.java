/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GuestbookResponse;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.confirmemail.ConfirmEmailData;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Query;

/**
 * Merges one account into another.
 * NOTE THAT THIS COMMAND IS INCOMPLETE AND DOES NOT PORT FOREIGN KEYS
 * This is done for the api after the command is called
 * 
 * @author matthew
 */

@RequiredPermissions({})
public class MergeInAccountCommand extends AbstractVoidCommand {
    final AuthenticatedUser consumedAU;
    final BuiltinUser consumedBU;
    final AuthenticatedUser ongoingAU;
    final String baseIdentifier;
    final String consumedIdentifier;
    final List<RoleAssignment> baseRAList;
    final List<RoleAssignment> consumedRAList;
    
    private static final Logger logger = Logger.getLogger(MergeInAccountCommand.class.getCanonicalName());
    
    public MergeInAccountCommand(DataverseRequest createDataverseRequest, String baseIdentifier, String consumedIdentifier, AuthenticatedUser consumedAuthenticatedUser, 
            BuiltinUser consumedBuiltinUser, AuthenticatedUser ongoingAU, List<RoleAssignment> baseRAList, List<RoleAssignment> consumedRAList) {
        super(
            createDataverseRequest,
            (DvObject) null
        );
        consumedAU = consumedAuthenticatedUser;
        consumedBU = consumedBuiltinUser;
        this.baseRAList = baseRAList;
        this.consumedRAList = consumedRAList;
        this.baseIdentifier = baseIdentifier;
        this.consumedIdentifier = consumedIdentifier;
        this.ongoingAU = ongoingAU;
    }
    
    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {
        
        //merge role assignments
        System.out.print(ctxt);
        for(RoleAssignment cra : consumedRAList) {
            if(cra.getAssigneeIdentifier().charAt(0) == '@') {
                //This still needs to check if the same Role Assignment exists in the baseRAList and if so... what?
                
                boolean willDelete = false;
                for(RoleAssignment bra : baseRAList) {
                    //Matching on the id not the whole DVObject as I'm suspicious of dvobject equality
                    if( bra.getDefinitionPoint().getId().equals(cra.getDefinitionPoint().getId())
                        && bra.getRole().equals(cra.getRole())) { //TODO: Match on privateurltoken as well?? What does this do?
                        willDelete = true; //more or less a skip, as we run a delete query afterwards
                    }
                }
                if(!willDelete) {
                    cra.setAssigneeIdentifier("@" + baseIdentifier);
                    ctxt.em().merge(cra);
                }
            } else {
                throw new IllegalCommandException("Original userIdentifier provided does not seem to be an AuthenticatedUser", this);
            }
        }
        
        //Delete role assignments for consumedIdentifier not merged, e.g. duplicates
        int resultCount = ctxt.em().createNamedQuery("RoleAssignment.deleteAllByAssigneeIdentifier", RoleAssignment.class).
                        setParameter("assigneeIdentifier", '@'+consumedIdentifier)
                        .executeUpdate();
        logger.log(Level.INFO, "Number of roles deleted : ({0})", resultCount);
        
        // DatasetVersionUser
        for (DatasetVersionUser user : ctxt.datasetVersion().getDatasetVersionUsersByAuthenticatedUser(consumedAU)) {
            user.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(user);
        }
        
        //DatasetLocks
        for (DatasetLock lock : ctxt.datasets().getDatasetLocksByUser(consumedAU)) {
            lock.setUser(ongoingAU);
            ctxt.em().merge(lock);
        }
        
        //DVObjects creator and release
        for (DvObject dvo : ctxt.dvObjects().findByAuthenticatedUserId(consumedAU)) {
            if (dvo.getCreator().equals(consumedAU)){
                dvo.setCreator(ongoingAU);
            }
            if (dvo.getReleaseUser() != null &&  dvo.getReleaseUser().equals(consumedAU)){
                dvo.setReleaseUser(ongoingAU);
            }
            ctxt.em().merge(dvo);
        }
        
        //GuestbookResponse
        for (GuestbookResponse gbr : ctxt.responses().findByAuthenticatedUserId(consumedAU)) {
            gbr.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(gbr);
        }
        
        //UserNotification
        for (UserNotification note : ctxt.notifications().findByUser(consumedAU.getId())) {
            note.setUser(ongoingAU);
            ctxt.em().merge(note);
        }
        
        //SavedSearch
        for (SavedSearch search : ctxt.savedSearches().findByAuthenticatedUser(consumedAU)) {
            search.setCreator(ongoingAU);
            ctxt.em().merge(search);
        }
        
        //Workflow Comments
        for (WorkflowComment wc : ctxt.authentication().getWorkflowCommentsByAuthenticatedUser(consumedAU)) {
            wc.setAuthenticatedUser(ongoingAU);
            ctxt.em().merge(wc);
        }
        
        //ConfirmEmailData  
        
        ConfirmEmailData confirmEmailData = ctxt.confirmEmail().findSingleConfirmEmailDataByUser(consumedAU);       
        ctxt.em().remove(confirmEmailData);
        
        //Access Request is not an entity. have to update with native query
        
        ctxt.em().createNativeQuery("UPDATE fileaccessrequests SET authenticated_user_id="+ongoingAU.getId()+" WHERE authenticated_user_id="+consumedAU.getId()).executeUpdate();
               
        
        //delete:
        //  builtin user
        //  authenticated user
        //  AuthenticatedUserLookup
        //  apiToken
        ApiToken toRemove = ctxt.authentication().findApiTokenByUser(consumedAU);
        ctxt.em().remove(toRemove);
        AuthenticatedUserLookup consumedAUL = consumedAU.getAuthenticatedUserLookup();
        ctxt.em().remove(consumedAUL);//do we need this with cascade?
        ctxt.em().remove(consumedAU);
        ctxt.em().remove(consumedBU);      
    }
    
}