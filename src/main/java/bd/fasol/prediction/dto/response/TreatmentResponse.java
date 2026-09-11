package bd.fasol.prediction.dto.response;
import bd.fasol.model.Treatment;
public record TreatmentResponse(Long id,String textBn,boolean organicPriority,String sourceNote,String verifiedBy) { public static TreatmentResponse from(Treatment t){return t==null?null:new TreatmentResponse(t.id,t.textBn,t.isOrganicPriority,t.sourceNote,t.verifiedBy);} }
