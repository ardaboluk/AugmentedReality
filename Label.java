package source;

public class Label {
	
	/*
	 * This class is an implementation of the Disjoint-Set data structure
	 * used by two-pass connected components labeling algorithm
	 * Reference: http://en.wikipedia.org/wiki/Disjoint-set_data_structure
	 * */
	
	private int labelName;
	private int rank;
	private Label parent;
	
	
	public Label(int labelName){
		
		this.labelName = labelName;
		this.parent = this;
		this.rank = 0;
	}
	
	public Label getRoot(){
		
		if(this.parent != this){
			
			this.parent = this.parent.getRoot();
		}
		
		return this.parent;
	}
	
	// order is important, label with bigger name should be
	// given as parameter
	public void union(Label label2){
		
		Label label1Root = this.getRoot();
		Label label2Root = label2.getRoot();
		if(label1Root == label2Root){
			return;
		}
		
		// if label1 and label2 are not already in the same set, merge them
		if(label1Root.getRank() < label2Root.getRank()){			
			label1Root.setParent(label2Root);
		}else if(label1Root.getRank() > label2Root.getRank()){
			label2Root.setParent(label1Root);
		}else{
			label2Root.setParent(label1Root);
			label1Root.setRank(label1Root.getRank() + 1);
		}
	}
	
	public void setRank(int rank){
		
		this.rank = rank;
	}
	
	public void setParent(Label parent){
		
		this.parent = parent;
	}
	
	public int getLabelName(){
		
		return this.labelName;
	}
	
	public int getRank(){
		
		return this.rank;
	}

}
